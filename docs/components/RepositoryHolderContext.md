# RepositoryHolderContext

## 何时使用

当需要在流程引擎框架层访问仓储和服务实例时使用。`RepositoryHolderContext` 是流程引擎的中央注册表，持有所有核心仓储和服务引用，使 framework 层代码无需直接依赖 Spring 容器即可访问基础设施。典型场景：

- 框架内部服务需要访问流程记录、延迟任务、操作人等数据
- 构建业务服务实例（`FlowService`、`FlowActionService`、`FlowDelayTriggerService`）
- 在 `flow-engine-starter` 自动配置阶段注册仓储实现
- 单元测试中手动注册内存实现

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.27</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `RepositoryHolderContext` | `com.codingapi.flow.context` | 仓库持有者单例，实现 `IRepositoryHolder` 接口，持有全部仓储和服务引用 |
| `IRepositoryHolder` | `com.codingapi.flow.session` | 资源持有对象接口，定义仓储和服务的访问方法 |

### 关键方法

#### 注册与校验

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getInstance()` | 无 | `RepositoryHolderContext` | 获取单例实例 |
| `register(WorkflowService, FlowRecordService, FlowOperatorGateway, ParallelBranchRepository, DelayTaskRepository, UrgeIntervalRepository, FlowOperatorAssignmentRepository)` | 七个仓储/服务实现 | `void` | 注册所有依赖（通常由 Starter 自动配置调用） |
| `isRegistered()` | 无 | `boolean` | 检查所有依赖是否已注册 |
| `verify()` | 无 | `void` | 校验注册状态，未注册时抛出 `FlowStateException` |

#### 服务工厂方法

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `createFlowService()` | 无 | `FlowService` | 构建流程服务 |
| `createFlowActionService(FlowSession)` | `flowSession` — 流程会话 | `FlowActionService` | 构建流程动作服务 |
| `createDelayTriggerService(DelayTask)` | `task` — 延迟任务 | `FlowDelayTriggerService` | 构建延迟触发执行服务 |

#### 流程记录操作

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `saveRecord(FlowRecord)` | `flowRecord` — 流程记录 | `void` | 保存单条流程记录 |
| `saveRecords(List<FlowRecord>)` | `flowRecords` — 流程记录列表 | `void` | 批量保存流程记录 |
| `getRecordById(long)` | `id` — 记录 ID | `FlowRecord` | 根据 ID 获取流程记录 |
| `findCurrentNodeRecords(long, String)` | `fromId` — 上级流程 ID, `nodeId` — 节点 ID | `List<FlowRecord>` | 查询当前节点下的流程记录 |
| `findProcessRecords(String)` | `processId` — 流程标识 | `List<FlowRecord>` | 查询流程标识下的全部记录 |
| `findAfterRecords(String, long)` | `processId` — 流程标识, `currentId` — 当前记录 ID | `List<FlowRecord>` | 查询指定记录之后的后续记录 |

#### 操作人查询

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getOperatorById(long)` | `id` — 操作人 ID | `IFlowOperator` | 获取单个操作人 |
| `findOperatorByIds(List<Long>)` | `ids` — 操作人 ID 列表 | `List<IFlowOperator>` | 批量获取操作人 |
| `saveOperatorAssignment(String, String, List<Long>)` | `processId`, `nodeId`, `operatorIds` | `void` | 保存节点操作人手动分配信息 |
| `findAssignedOperatorIds(String, String)` | `processId`, `nodeId` | `List<Long>` | 查询节点已分配的操作人 ID |

#### 延迟任务操作

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `saveDelayTask(DelayTask)` | `delayTask` — 延迟任务 | `void` | 保存延迟任务 |
| `deleteDelayTask(DelayTask)` | `delayTask` — 延迟任务 | `void` | 删除延迟任务 |
| `findDelayTasks()` | 无 | `List<DelayTask>` | 获取全部延迟任务 |

#### 并行分支操作

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getParallelBranchTriggerCount(String)` | `parallelId` — 并行 ID | `int` | 获取并行分支触发总数 |
| `addParallelTriggerCount(String)` | `parallelId` — 并行 ID | `void` | 递增并行分支触发计数 |
| `clearParallelTriggerCount(String)` | `parallelId` — 并行 ID | `void` | 清空并行分支触发计数 |

#### 催办操作

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `saveUrgeInterval(UrgeInterval)` | `interval` — 催办间隔控制 | `void` | 保存催办控制 |
| `getLatestUrgeInterval(String, long)` | `processId` — 流程标识, `recordId` — 记录 ID | `UrgeInterval` | 获取最新的催办控制 |

## 使用示例

```java
// 基础用法：获取单例并访问服务
RepositoryHolderContext context = RepositoryHolderContext.getInstance();
context.verify(); // 确保已注册

FlowService flowService = context.createFlowService();
FlowRecord record = context.getRecordById(1001L);
List<IFlowOperator> operators = context.findOperatorByIds(List.of(1L, 2L, 3L));
```

```java
// Spring Boot 自动配置中注册（flow-engine-starter 内部调用）
@Component
public class RepositoryRegister implements InitializingBean {

    private final WorkflowService workflowService;
    private final FlowRecordService flowRecordService;
    private final FlowOperatorGateway flowOperatorGateway;
    private final ParallelBranchRepository parallelBranchRepository;
    private final DelayTaskRepository delayTaskRepository;
    private final UrgeIntervalRepository urgeIntervalRepository;
    private final FlowOperatorAssignmentRepository flowOperatorAssignmentRepository;

    // 构造器注入...

    @Override
    public void afterPropertiesSet() {
        RepositoryHolderContext.getInstance().register(
            workflowService,
            flowRecordService,
            flowOperatorGateway,
            parallelBranchRepository,
            delayTaskRepository,
            urgeIntervalRepository,
            flowOperatorAssignmentRepository
        );
    }
}
```

```java
// 单元测试中手动注册
RepositoryHolderContext ctx = RepositoryHolderContext.getInstance();
ctx.register(
    inMemoryWorkflowService,
    inMemoryFlowRecordService,
    inMemoryFlowOperatorGateway,
    inMemoryParallelBranchRepo,
    inMemoryDelayTaskRepo,
    inMemoryUrgeIntervalRepo,
    inMemoryFlowOperatorAssignmentRepo
);
// 现在可以安全调用 ctx 的所有方法
```

## 注意事项

- **注册时机**：必须在应用启动阶段完成注册（由 `flow-engine-starter` 的 `RepositoryHolderContextRegister` 自动执行），未注册时调用 `verify()` 会抛出 `FlowStateException`
- **线程安全性**：单例使用饿汉式初始化，但 `register()` 方法无同步保护，应确保仅调用一次且在启动阶段完成
- **全量注册**：`register()` 要求一次性提供全部七个依赖，不支持部分注册
- **框架内部使用**：`RepositoryHolderContext` 主要供框架内部服务使用，业务代码通常通过 `FlowService` 等高层 API 间接访问
