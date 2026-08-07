---
name: flow-engine-framework/repository
module: flow-engine-framework
description: 仓储抽象层，定义流程引擎的持久化接口和仓储持有者上下文
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - WorkflowRepository
  - WorkflowVersionRepository
  - WorkflowRuntimeRepository
  - FlowRecordRepository
  - FlowTodoRecordRepository
  - FlowTodoMergeRepository
  - FlowOperatorAssignmentRepository
  - ParallelBranchRepository
  - DelayTaskRepository
  - UrgeIntervalRepository
  - NodeViewJavaScriptRepository
  - IRepositoryHolder
content_hash: 057987ed8ab209759993dfc2792699ccdcd110a1c291ffd49fea7af09e1af895
---

## 解决什么问题

提供流程引擎的持久化抽象层，解决以下问题：

- **持久化解耦**：框架层只定义 Repository 接口，不依赖具体的 ORM 实现，由 `flow-engine-starter-infra` 提供 JPA 实现
- **仓储持有**：`IRepositoryHolder` 作为所有 Repository 的统一持有者，通过依赖注入一次性获取所有仓储
- **多数据源支持**：不同 Repository 可对接不同的数据源实现

## 如何使用

### Repository 接口清单

| 接口 | 职责 |
|------|------|
| `WorkflowRepository` | 流程定义的 CRUD |
| `WorkflowVersionRepository` | 流程版本的 CRUD |
| `WorkflowRuntimeRepository` | 流程运行时快照的 CRUD |
| `FlowRecordRepository` | 流程执行记录的 CRUD |
| `FlowTodoRecordRepository` | 待办记录的 CRUD |
| `FlowTodoMergeRepository` | 待办合并关系的 CRUD |
| `FlowOperatorAssignmentRepository` | 操作者分配记录的 CRUD |
| `ParallelBranchRepository` | 并行分支状态的 CRUD |
| `DelayTaskRepository` | 延迟任务的 CRUD |
| `UrgeIntervalRepository` | 催办间隔配置的 CRUD |
| `NodeViewJavaScriptRepository` | 节点视图 JS 脚本的 CRUD |
| `SubProcessRepository` | 子流程执行记录的 CRUD（子流程聚合持久化依赖） |

### IRepositoryHolder

`IRepositoryHolder` 是所有 Repository 的统一访问入口，通过 `RepositoryHolderContext` 注册和获取。它对外暴露的是服务与业务方法，而非各 Repository 的 getter：

```java
public interface IRepositoryHolder {
    WorkflowService getWorkflowService();
    FlowRecordService getFlowRecordService();
    FlowOperatorGateway getFlowOperatorGateway();
    SubProcessRepository getSubProcessRepository();
    void saveRecords(List<FlowRecord> flowRecords);
    List<FlowRecord> findProcessRecords(String processId);
    // ... 其余业务方法
}
```

### 扩展新 Repository

1. 在 `flow-engine-framework` 中定义 Repository 接口
2. 在 `flow-engine-starter-infra` 中提供 JPA 实现
3. 在 `IRepositoryHolder` 中添加对应的 getter 方法
4. 在 `RepositoryHolderContext` 中注册实现

## 使用实例

```java
// 通过 IRepositoryHolder 获取服务（单例上下文）
IRepositoryHolder holder = RepositoryHolderContext.getInstance();
WorkflowService workflowService = holder.getWorkflowService();
FlowRecordService recordService = holder.getFlowRecordService();

// 查询流程定义
Workflow workflow = workflowService.getWorkflowByCode("leave-request");

// 保存流程记录
holder.saveRecords(flowRecords);

// 实现自定义 Repository（在 infra 模块中）
@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {
    @Autowired
    private WorkflowEntityRepository jpaRepo;

    @Override
    public Workflow getByCode(String code) {
        return jpaRepo.getWorkflowEntityByCode(code).toWorkflow();
    }
}
```
