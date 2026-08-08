# 仓储抽象与持久化

引擎核心（`flow-engine-framework`）与 Spring 无关，所有存储/服务访问统一经过 `IRepositoryHolder` 抽象。持久化层由 `flow-engine-starter-infra` 提供 JPA 实现；业务方也可以自行实现仓储接口替换存储。

## 1. 资源持有者抽象：`IRepositoryHolder`

文件：`flow-engine-framework/src/main/java/com/codingapi/flow/session/IRepositoryHolder.java`

引擎各服务（`FlowXxxService`）不直接注入仓储，而是通过 `IRepositoryHolder` 获取依赖：

```java
public interface IRepositoryHolder {
    SubProcessRepository getSubProcessRepository();
    WorkflowService getWorkflowService();
    FlowRecordService getFlowRecordService();
    FlowOperatorGateway getFlowOperatorGateway();
    FlowDelayTriggerService createDelayTriggerService(DelayTask task);
    FlowActionService createFlowActionService(FlowSession flowSession);
    FlowService createFlowService();
    FlowRecord getRecordById(long recordId);
    List<IFlowOperator> findOperatorByIds(List<Long> ids);
    IFlowOperator getOperatorById(long id);
    void saveDelayTask(DelayTask delayTask);
    void deleteDelayTask(DelayTask delayTask);
    void saveRecords(List<FlowRecord> flowRecords);
    void saveRecord(FlowRecord flowRecord);
    List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId);
    List<FlowRecord> findProcessRecords(String processId);
    List<FlowRecord> findAfterRecords(String processId, long currentId);
    int getParallelBranchTriggerCount(String parallelId);
    void addParallelTriggerCount(String parallelId);
    void clearParallelTriggerCount(String parallelId);
    void saveUrgeInterval(UrgeInterval interval);
    UrgeInterval getLatestUrgeInterval(String processId, long recordId);
    List<DelayTask> findDelayTasks();
    void saveOperatorAssignment(String processId, String nodeId, List<Long> operatorIds);
    List<Long> findAssignedOperatorIds(String processId, String nodeId);
}
```

### 两个实现

| 实现 | 场景 | 说明 |
|---|---|---|
| `RepositoryHolderContext`（`context/` 包，单例） | 生产 | 由 starter 的 `RepositoryHolderContextRegister` 注册 7 个依赖；操作人读取委托 `GatewayContext`；记录保存委托 `FlowRecordService` |
| `MockRepositoryHolder`（`mock/` 包） | Mock 沙箱 | 构造时 new 出 9 个内存仓储 + `FlowRecordService`/`WorkflowService`，每个 Mock 实例独立一份（见 [Mock 模式](./mock-mode.md)） |

## 2. 仓储接口（12 个）

文件：`flow-engine-framework/src/main/java/com/codingapi/flow/repository/`

### 2.1 流程设计相关

```java
public interface WorkflowRepository {
    void save(Workflow workflow);
    Workflow getById(String id);
    Workflow getByCode(String code);
    default void lockById(String id) {}   // 持久化仓储应实现数据库行锁，保证多实例下流程维度创建串行
    void delete(String id);
}

public interface WorkflowVersionRepository {
    WorkflowVersion get(long id);
    void delete(String workId);
    List<WorkflowVersion> findVersion(String workId);
    void saveAll(List<WorkflowVersion> versionList);
    void save(WorkflowVersion workflowVersion);
    void delete(long id);
}

public interface WorkflowRuntimeRepository {
    void save(WorkflowRuntime workflowRuntime);
    WorkflowRuntime get(long id);
    WorkflowRuntime getByWorkId(String workId, long workVersion);
    void delete(WorkflowRuntime backup);
}
```

> **`WorkflowRepository.lockById`**：接口 javadoc 明确要求持久化实现使用数据库行锁（多实例部署需要）。JPA 实现 `WorkflowRepositoryImpl` 已实现；`WorkflowService.getOrCreateWorkflowRuntime` 通过 `@Transactional(REQUIRES_NEW)` + `lockById` 保证流程维度创建串行。

### 2.2 流程记录相关

```java
public interface FlowRecordRepository {
    FlowRecord get(long id);
    List<FlowRecord> findByIds(List<Long> ids);
    void save(FlowRecord flowRecord);          // 注意：为保证待办合并一致性，保存应经由 IRepositoryHolder#saveRecord
    void saveAll(List<FlowRecord> flowRecords);
    void delete(FlowRecord flowRecord);
    List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId);
    List<FlowRecord> findProcessRecords(String processId);
    List<FlowRecord> findTodoRecords(String processId);
    List<FlowRecord> findAfterRecords(String processId, long fromId);
    List<FlowRecord> findBeforeRecords(String processId, long id);
}

public interface FlowTodoRecordRepository {
    void saveAll(List<FlowTodoRecord> margeRecords);
    FlowTodoRecord getByTodoKey(String key);
    void delete(FlowTodoRecord margeRecord);
    void save(FlowTodoRecord margeRecord);
}

public interface FlowTodoMergeRepository {
    void saveAll(List<FlowTodoMerge> list);
    void delete(FlowTodoMerge todoMerge);
    List<FlowTodoMerge> findByTodoId(long todoId);
}
```

### 2.3 功能支撑相关

```java
/** 操作人手动分配（INITIATOR_SELECT / APPROVER_SELECT） */
public interface FlowOperatorAssignmentRepository {
    void save(String processId, String nodeId, List<Long> operatorIds);   // 幂等，已存在则覆盖
    List<Long> findOperatorIds(String processId, String nodeId);          // 不存在时返回空列表
}

/** 节点视图 JS 持久化（⚠️ infra 不提供实现，必须由业务方提供） */
public interface NodeViewJavaScriptRepository {
    void save(NodeViewJavaScript javaScript);
    void delete(String code);
    NodeViewJavaScript get(String code);
}

/** 并行分支触发计数 */
public interface ParallelBranchRepository {
    int getTriggerCount(String parallelId);
    void addTriggerCount(String parallelId);
    void clearTriggerCount(String parallelId);
}

/** 子流程记录 */
public interface SubProcessRepository {
    void save(SubProcessRecord record);
    List<SubProcessRecord> findByParentRecordId(long parentRecordId);
    List<SubProcessRecord> findByParentProcessId(String parentProcessId);
    List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId);
}

/** 延迟任务 */
public interface DelayTaskRepository {
    void save(DelayTask task);
    void delete(DelayTask delayTask);
    List<DelayTask> findAll();
}

/** 催办间隔 */
public interface UrgeIntervalRepository {
    UrgeInterval getLatest(String processId, long recordId);
    void save(UrgeInterval urgeInterval);
}
```

## 3. JPA 实现（`flow-engine-starter-infra`）

### 3.1 自动装配

- `AutoConfiguration`：`@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")` + `@Import(FlowJpaPackageRegistrar.class)`。
- `FlowJpaPackageRegistrar`（`ImportBeanDefinitionRegistrar`）调用 `AutoConfigurationPackages.register(registry, "com.codingapi.flow.infra")`，让宿主应用的 JPA 扫描覆盖引擎实体与仓储包 —— **业务方无需配置 `@EntityScan`**。

### 3.2 仓储实现（11 个 Bean）

| Bean | 实现类 |
|---|---|
| `delayTaskRepository` | `DelayTaskRepositoryImpl` |
| `urgeIntervalRepository` | `UrgeIntervalRepositoryImpl` |
| `parallelBranchRepository` | `ParallelBranchRepositoryImpl` |
| `workflowRepository` | `WorkflowRepositoryImpl` |
| `workflowRuntimeRepository` | `WorkflowRuntimeRepositoryImpl` |
| `workflowVersionRepository` | `WorkflowVersionRepositoryImpl` |
| `flowRecordRepository` | `FlowRecordRepositoryImpl` |
| `flowTodoRecordRepository` | `FlowTodoRecordRepositoryImpl` |
| `flowTodoMargeRepository`（bean 名 marge） | `FlowTodoMergeRepositoryImpl` |
| `flowOperatorAssignmentRepository` | `FlowOperatorAssignmentRepositoryImpl` |
| `subProcessRepository` | `SubProcessRepositoryImpl` |

> ⚠️ **infra 不提供** `NodeViewJavaScriptRepository` 与 `FlowOperatorGateway` 的实现，必须由业务方提供。

### 3.3 JPA 实体与表

| 实体类 | 表名 |
|---|---|
| `WorkflowEntity` | `t_flow_workflow` |
| `WorkflowVersionEntity` | `t_flow_workflow_version` |
| `WorkflowRuntimeEntity` | `t_flow_workflow_runtime`（唯一约束 `uk_workflow_runtime_work_version(workId, workVersion)`） |
| `FlowRecordEntity` | `t_flow_record` |
| `FlowTodoRecordEntity` | `t_flow_todo_record` |
| `FlowTodoMargeEntity` | `t_flow_todo_marge` |
| `ParallelControlEntity` | `t_flow_parallel_control` |
| `DelayTaskEntity` | `t_flow_delay_task` |
| `UrgeIntervalEntity` | `t_flow_urge_interval` |
| `FlowOperatorAssignmentEntity` | `t_flow_operator_assignment` |
| `SubProcessRecordEntity` | `t_flow_sub_process_record` |

Spring Data 仓储接口位于 `jpa/` 包，全部继承外部 `FastRepository`（来自 `springboot-starter-data-fast`）。

## 4. 记录保存一致性（`FlowRecordSaveService`）

三类记录紧密绑定，必须保持一致：

- `FlowRecord` — 流程执行记录（TODO / DONE 状态）
- `FlowTodoRecord` — 待办记录
- `FlowTodoMerge` — 待办合并关系（开启记录合并 `isMergeable()` 时产生）

保存统一走 `FlowRecordSaveService.saveAll()`，单次调用按 `saveRecords → saveTodoMargeRecords → removeTodoMergeRecords` 顺序执行。

## 5. 脚本持久化（业务方必须提供）

来自外部 `springboot-starter-script` 的 SPI：

| SPI | 方法 | 注册方式 |
|---|---|---|
| `GroovyScriptRepository` | `save / delete / get(String key)` | `GroovyScriptRepositoryContext.getInstance().setGroovyScriptRepository(...)` |
| `TempGroovyScriptRepository` | `get / save / delete / find(PageRequest)` | `TempGroovyScriptRepositoryContext.getInstance().setTempGroovyScriptRepository(...)` |

参考实现（`flow-engine-example/.../repository/impl/`）：

```java
@Repository
public class GroovyScriptRepositoryImpl implements GroovyScriptRepository, InitializingBean {
    private final GroovyScriptEntityRepository repository;
    // ...
    @Override
    public void afterPropertiesSet() {
        GroovyScriptRepositoryContext.getInstance().setGroovyScriptRepository(this);
    }
}
```

## 6. 替换存储

- **引入 `flow-engine-starter-infra`**：开箱即用（推荐）。
- **不引入 infra**：自行实现 11 个仓储接口 + `NodeViewJavaScriptRepository` 为 Spring Bean 即可。starter 的 `RepositoryHolderContextRegister` 会自动注入。framework 测试包（`src/test/java/com/codingapi/flow/repository/`）提供了一套内存实现可作参考。

## 7. 离线/最小装配（脱离 Spring）

framework 测试中的 `MyFlowServiceFactory` 展示了最小装配路径 —— 仅两次静态单例注入：

```java
// 1. 注册仓储/服务
RepositoryHolderContext.getInstance().register(
        workflowService, flowRecordService,
        parallelBranchRepository, delayTaskRepository,
        urgeIntervalRepository, flowOperatorAssignmentRepository,
        subProcessRepository);
// 2. 注册操作人网关
GatewayContext.getInstance().setFlowOperatorGateway(userGateway);
```