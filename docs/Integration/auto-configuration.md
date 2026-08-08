# Spring Boot 自动配置

`flow-engine-starter` 通过 Spring Boot 自动配置把引擎单例与 Spring 容器打通。注册入口见 `flow-engine-starter/src/main/java/com/codingapi/flow/AutoConfiguration.java`。

## 1. 注册方式

双注册文件（Boot2 / Boot3 兼容）：

- `META-INF/spring.factories` → `EnableAutoConfiguration=com.codingapi.flow.AutoConfiguration`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` → `com.codingapi.flow.AutoConfiguration`

`AutoConfiguration` 为 `@Configuration`，**无 `@ConditionalOnMissingBean`、无条件注解** —— 构造函数依赖的 Bean 缺失将导致启动失败。

## 2. 注册的 Bean 全清单

| @Bean | 依赖 | 作用 |
|---|---|---|
| `NodeViewJavaScriptCacheContextRegister` | **`NodeViewJavaScriptRepository`** | 构造器内执行 `NodeViewJavaScriptCacheContext.getInstance().setNodeViewJavaScriptRepository(repo)`（15 分钟缓存上下文） |
| `GatewayContextRegister` | **`FlowOperatorGateway`** | 把操作人网关灌入 `GatewayContext` 单例（`InitializingBean`） |
| `FlowScriptContextRegister` | `ApplicationContext` + **`FlowOperatorGateway`** + `FlowRecordRepository` | 给 `FlowScriptContext` 单例设置 `IBeanFactory`（转发 Spring 容器，脚本 `$bind` 依赖） |
| `RepositoryHolderContextRegister` | `WorkflowService` + `FlowRecordService` + 5 个仓储 + `GatewayContextRegister` | 把 7 个依赖灌入 `RepositoryHolderContext` 单例（`InitializingBean`） |
| `FlowRecordService` | `FlowTodoRecordRepository` + `FlowTodoMergeRepository` + `FlowRecordRepository` | 记录服务 Bean |
| `WorkflowService` | `WorkflowVersionRepository` + `WorkflowRepository` + `WorkflowRuntimeRepository` | 流程设计服务 Bean |
| `FlowService` | `RepositoryHolderContextRegister`（仅保证顺序） | `new FlowService(RepositoryHolderContext.getInstance())`，业务入口 |
| `FlowDelayTaskRunner` | `RepositoryHolderContextRegister` | `ApplicationRunner + DisposableBean`：启动时 `DelayTaskManager.getInstance().start(...)` 加载延迟任务，销毁时 `close()` |

## 3. Register 类（单例注入）

全部位于 `flow-engine-starter/src/main/java/com/codingapi/flow/register/`，均实现 `InitializingBean`：

```java
// GatewayContextRegister
@Override
public void afterPropertiesSet() {
    GatewayContext.getInstance().setFlowOperatorGateway(flowOperatorGateway);
}

// RepositoryHolderContextRegister
@Override
public void afterPropertiesSet() {
    RepositoryHolderContext.getInstance().register(
            workflowService, flowRecordService,
            parallelBranchRepository, delayTaskRepository,
            urgeIntervalRepository, flowOperatorAssignmentRepository,
            subProcessRepository);
}

// FlowScriptContextRegister
@Override
public void afterPropertiesSet() {
    FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
        // getBean/getBeans 转发 Spring 容器
        // getRecordById 走 flowRecordRepository.get
        // getOperatorById/findOperatorsByIds 走 flowOperatorGateway
    });
}
```

## 4. api / query 模块装配

| 模块 | 装配 |
|---|---|
| `flow-engine-starter-api` | `@Configuration @ComponentScan("com.codingapi.flow.api")`。`WorkflowController` 注入 `FlowOperatorGateway` + `WorkflowRepository` 用于创建 Mock 实例；当前用户经 `UserContext.getInstance().current()` 强转 `IFlowOperator` |
| `flow-engine-starter-query` | `@Configuration @ComponentScan("com.codingapi.flow.query")` + `@Bean FlowRecordQueryService(FlowRecordEntityRepository, FlowTodoRecordEntityRepository)` → `FlowRecordQueryServiceImpl`（直接依赖 infra 的 JPA 仓储） |

## 5. 必选 / 可选实现清单

### 必须提供（缺失即启动失败）

| # | 接口/Bean | 说明 | 注入通道 |
|---|---|---|---|
| 1 | `FlowOperatorGateway` | 对接应用用户体系（`get`/`findByIds`） | `GatewayContextRegister` → `GatewayContext`；同时被 `FlowScriptContextRegister`、api Controller 依赖 |
| 2 | `IFlowOperator` 实现类 | 用户实体/DTO | 由 Gateway 返回，经 `FlowOperatorLocalThreadCache` 缓存 |
| 3 | `NodeViewJavaScriptRepository` | 节点视图 JS 持久化（infra 无实现） | `NodeViewJavaScriptCacheContextRegister` 构造器 |
| 4 | 11 个仓储接口实现 | 引入 `flow-engine-starter-infra` 则自动提供；否则自行实现 | `RepositoryHolderContextRegister` / `FlowRecordService` / `WorkflowService` |
| 5 | `GroovyScriptRepository` + `TempGroovyScriptRepository` | 脚本持久化 SPI（外部 starter-script） | 自注册到对应 `*RepositoryContext` 单例 |

### 可选（有默认实现 / 按需替换）

| # | 扩展点 | 默认行为 | 替换方式 |
|---|---|---|---|
| 1 | `FlowIDGeneratorGateway` | 随机字母数字（18/10 位）；`generateRecordId()` 默认返回 0（依赖 DB 自增） | `FlowIDGeneratorGatewayContext.getInstance().setFlowIDGeneratorGateway(...)` |
| 2 | `WorkflowRepository.lockById` | default 空实现 | 持久化实现应使用数据库行锁（多实例部署） |
| 3 | 当前登录用户 | — | `UserContext.getInstance().setCurrent(...)`（配合安全框架） |
| 4 | 默认脚本 | `DefaultScriptRegistry` | `ScriptRegistryContext.getInstance().setRegistry(...)` |
| 5 | 事件事务分发 | `SpringDefaultEventHandler`（发布即分发） | `codingapi.framework.event.transaction.enable=true` → 事务提交后分发 |

## 6. 配置项

| 配置前缀 | 作用 |
|---|---|
| `codingapi.script.tempValidTime` | 临时脚本有效期（默认 15 分钟） |
| `codingapi.script.shellMaxCacheSize` | 脚本编译缓存上限（默认 10240） |
| `codingapi.framework.handler-thread-pool-size` | 异步事件线程池大小（默认 20） |
| `codingapi.framework.event.transaction.enable` | 事件事务提交后分发开关（默认 false） |