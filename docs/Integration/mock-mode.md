# Mock 模式

Mock 模式为引擎提供的**全内存沙箱**：不触碰生产仓储，便于演示、原型与自动化测试。技术上适用于「无真实用户体系 / 无数据库」的场景。

## 1. 核心机制

```
POST /api/cmd/workflow/mock（需流程管理员）→ 返回 mockKey
  → MockInstanceFactory.create(flowOperatorGateway, workflowRepository)
    → 生成 mockKey（FlowIDGeneratorGateway.generateMockKey()，18 位）
    → new MockRepositoryHolder(flowOperatorGateway, workflowRepository)
      → 内部 new 出 9 个内存 Mock 仓储 + FlowRecordService + WorkflowService
    → new FlowService(mockRepositoryHolder)
    → new FlowRecordQueryMockService(...)
    → MockInstance 缓存进 ConcurrentHashMap
  → 后续请求带 ?mockKey=xxx → 分流到该 MockInstance
```

## 2. 关键类

### 2.1 `MockInstanceFactory`（`mock/MockInstanceFactory.java`，单例）

```java
public MockInstance create(FlowOperatorGateway flowOperatorGateway, WorkflowRepository workflowRepository);
public MockInstance getMockInstance(String key);   // 访问即续期
public void clear(String key);                     // 注销并 cancel 过期调度
```

### 2.2 `MockInstance`（`mock/MockInstance.java`）

- 持有 `mockKey` / `MockRepositoryHolder` / `FlowService` / `FlowRecordQueryService`。
- **15 分钟无操作自动过期**（`MAX_KEEP_TIME = 1000 * 60 * 15`），共享单线程守护调度每秒检查。

### 2.3 `MockRepositoryHolder`（`mock/MockRepositoryHolder.java`）

- 实现 `IRepositoryHolder`，构造时 `new` 出 9 个内存仓储（`mock/repository/` 下）：
  `DelayTaskRepositoryMockImpl`、`FlowRecordRepositoryMockImpl`、`FlowTodoMergeRepositoryMockImpl`、`FlowTodoRecordRepositoryMockImpl`、`ParallelBranchRepositoryMockImpl`、`SubProcessRepositoryMockImpl`、`UrgeIntervalRepositoryMockImpl`、`WorkflowRuntimeRepositoryMockImpl`、`WorkflowVersionRepositoryMockImpl`。
- 流程设计（`WorkflowRepository`）使用**真实传入**的仓储 —— Mock 模式可复用生产已保存的流程设计。
- `operatorAssignmentCache`（HashMap，key = `processId:nodeId`）模拟「发起人设定操作人」分配。
- `createFlowActionService(session)` 传 `allowDisabledAction = true`（Mock 下允许执行停用流程的动作）。

## 3. 接入方式

### 3.1 REST 方式

```bash
# 1. 创建 Mock 沙箱（需流程管理员）
curl -X POST /api/cmd/workflow/mock
# → { "data": "mockKey" }

# 2. 发起流程（带 mockKey 分流 + operatorId 指定用户）
curl -X POST "/api/cmd/record/create?mockKey=xxx&operatorId=1" \
  -H "Content-Type: application/json" \
  -d '{"workCode":"leave_flow","formData":{}}'

# 3. 查询 / 审批同样带 mockKey
curl -X POST "/api/cmd/record/action?mockKey=xxx&operatorId=1" \
  -H "Content-Type: application/json" \
  -d '{"recordId":123,"advice":{"actionId":"xxx"}}'

# 4. 清理
curl -X POST /api/cmd/workflow/cleanMock -d '{"id":"mockKey"}'
```

### 3.2 编程方式

```java
MockInstance mockInstance = MockInstanceFactory.getInstance()
        .create(flowOperatorGateway, workflowRepository);
String mockKey = mockInstance.getMockKey();
long recordId = mockInstance.getFlowService().create(request);
```

## 4. 关键行为

| 行为 | 说明 |
|---|---|
| 事件携带 mock 标记 | 所有事件 `isMock() == true`，订阅方可据此过滤（不发送真实消息） |
| 记录 id | 内存自增（`FlowRecordRepositoryMockImpl`），不依赖 DB |
| 查询 | `FlowRecordQueryMockService` 实现 `FlowRecordQueryService`，recordId 降序内存分页 |
| 流程设计共享 | 复用宿主应用的 `WorkflowRepository`（真实存储） |
| 过期 | 15 分钟无操作自动销毁；`getMockInstance` 访问即续期 |
| 权限校验 | 创建 Mock 需当前用户为流程管理员（`UserContext` 有值且 `isFlowManager()`；无登录态时放行） |