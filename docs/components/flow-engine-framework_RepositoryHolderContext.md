---
module: flow-engine-framework
name: RepositoryHolderContext
description: 流程引擎仓库持有者上下文，以单例模式聚合并管理所有仓储实例和服务，是框架运行时的基础设施注册中心。
---

# RepositoryHolderContext

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

RepositoryHolderContext 是流程引擎运行时的基础设施注册中心。在 Spring Boot 应用启动时，由 starter 层的 `RepositoryHolderContextRegister` 自动将所有仓储实例和服务注册到该单例中。框架内部通过此上下文获取流程记录服务、操作人网关、并行分支仓储等基础设施，是连接框架层与基础设施层的核心桥梁。

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.30</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `RepositoryHolderContext` | `com.codingapi.flow.context` | 仓库持有者上下文单例，实现 IRepositoryHolder 接口 |
| `IRepositoryHolder` | `com.codingapi.flow.session` | 资源持有对象接口，定义框架所需的全部数据操作方法 |
| `MockRepositoryHolder` | `com.codingapi.flow.mock` | 测试用模拟实现，使用内存存储 |
| `RepositoryHolderContextRegister` | `com.codingapi.flow.register` | Spring 初始化回调，负责将 Bean 注册到 RepositoryHolderContext |

### 关键方法

#### 注册与验证

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `static getInstance()` | 无 | `RepositoryHolderContext` | 获取单例实例 |
| `register(WorkflowService, FlowRecordService, FlowOperatorGateway, ParallelBranchRepository, DelayTaskRepository, UrgeIntervalRepository, FlowOperatorAssignmentRepository)` | 各仓储和服务实例 | `void` | 注册所有依赖（由 starter 层自动调用） |
| `isRegistered()` | 无 | `boolean` | 检查所有必需依赖是否已注册 |
| `verify()` | 无 | `void` | 验证注册状态，未注册时抛出 FlowStateException |

#### 持有的服务与仓储（Getter）

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getWorkflowService()` | `WorkflowService` | 流程设计服务 |
| `getFlowRecordService()` | `FlowRecordService` | 流程记录服务 |
| `getFlowOperatorGateway()` | `FlowOperatorGateway` | 操作者防腐层网关 |
| `getParallelBranchRepository()` | `ParallelBranchRepository` | 并行分支仓储 |
| `getDelayTaskRepository()` | `DelayTaskRepository` | 延迟任务仓储 |
| `getUrgeIntervalRepository()` | `UrgeIntervalRepository` | 催办间隔仓储 |
| `getFlowOperatorAssignmentRepository()` | `FlowOperatorAssignmentRepository` | 操作人分配仓储 |

#### 服务工厂方法

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `createFlowService()` | 无 | `FlowService` | 构建流程服务实例 |
| `createFlowActionService(FlowSession)` | flowSession - 流程会话 | `FlowActionService` | 构建流程动作服务 |
| `createDelayTriggerService(DelayTask)` | task - 延迟任务 | `FlowDelayTriggerService` | 构建延迟触发服务 |

#### 数据操作方法（委托给各仓储和服务）

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getRecordById(long)` | `FlowRecord` | 获取流程记录 |
| `getOperatorById(long)` | `IFlowOperator` | 获取操作人 |
| `findOperatorByIds(List<Long>)` | `List<IFlowOperator>` | 批量查询操作人 |
| `saveRecord(FlowRecord)` | `void` | 保存流程记录 |
| `saveRecords(List<FlowRecord>)` | `void` | 批量保存流程记录 |
| `findCurrentNodeRecords(long, String)` | `List<FlowRecord>` | 查询当前节点记录 |
| `findProcessRecords(String)` | `List<FlowRecord>` | 按 processId 查询记录 |
| `findAfterRecords(String, long)` | `List<FlowRecord>` | 查询后续记录 |
| `saveDelayTask(DelayTask)` | `void` | 保存延迟任务 |
| `deleteDelayTask(DelayTask)` | `void` | 删除延迟任务 |
| `findDelayTasks()` | `List<DelayTask>` | 查询全部延迟任务 |
| `getParallelBranchTriggerCount(String)` | `int` | 获取并行分支触发计数 |
| `addParallelTriggerCount(String)` | `void` | 增加并行分支触发计数 |
| `clearParallelTriggerCount(String)` | `void` | 清空并行分支触发计数 |
| `saveUrgeInterval(UrgeInterval)` | `void` | 保存催办间隔 |
| `getLatestUrgeInterval(String, long)` | `UrgeInterval` | 获取最新催办间隔 |
| `saveOperatorAssignment(String, String, List<Long>)` | `void` | 保存节点操作人分配 |
| `findAssignedOperatorIds(String, String)` | `List<Long>` | 查询已分配的操作人 |

### 注册流程

```
Spring Boot 启动
  → AutoConfiguration 创建 RepositoryHolderContextRegister（注入所有仓储 Bean）
    → afterPropertiesSet() 回调
      → RepositoryHolderContext.getInstance().register(...)
        → 框架运行时可通过 getInstance() 获取所有基础设施
```

### 配置项

RepositoryHolderContext 自身无配置项。所有依赖通过 Spring Bean 注入。

## 使用示例

### Spring Boot 自动注册（starter 层自动完成）

```java
// AutoConfiguration 中自动完成，业务方无需手动调用
@Configuration
public class AutoConfiguration {

    @Bean
    public RepositoryHolderContextRegister repositoryHolderContextRegister(
            WorkflowService workflowService,
            FlowRecordService flowRecordService,
            FlowOperatorGateway flowOperatorGateway,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository,
            FlowOperatorAssignmentRepository flowOperatorAssignmentRepository,
            GatewayContextRegister gatewayContextRegister) {
        return new RepositoryHolderContextRegister(
                workflowService, flowRecordService, flowOperatorGateway,
                parallelBranchRepository, delayTaskRepository,
                urgeIntervalRepository, flowOperatorAssignmentRepository,
                gatewayContextRegister);
    }
}
```

### 获取流程服务

```java
// 通过单例获取已注册的服务
FlowService flowService = RepositoryHolderContext.getInstance().createFlowService();
```

### 测试中的注册

```java
// 测试环境手动注册
RepositoryHolderContext.getInstance().register(
    workflowService, flowRecordService, userGateway,
    parallelBranchRepository, delayTaskRepository,
    urgeIntervalRepository, flowOperatorAssignmentRepository
);
IRepositoryHolder holder = RepositoryHolderContext.getInstance();
```

## 注意事项

- **启动顺序**：必须在 Spring 容器初始化完成后才能使用，`RepositoryHolderContextRegister` 通过 `InitializingBean.afterPropertiesSet()` 在 Bean 属性设置完成后自动注册
- **注册验证**：调用 `verify()` 或任何工厂方法前需确保 `isRegistered()` 返回 true，否则抛出 `FlowStateException`
- **单例模式**：全局唯一实例，通过 `getInstance()` 获取，不可直接构造
- **IRepositoryHolder 接口**：框架内部多数场景通过 IRepositoryHolder 接口而非具体类访问资源，便于测试时使用 MockRepositoryHolder 替代
- **不可重复注册**：register() 会直接覆盖已有实例，正常启动流程中只应调用一次
