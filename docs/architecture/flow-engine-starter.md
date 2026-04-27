# flow-engine-starter

Spring Boot 自动配置入口模块，聚合所有子模块并完成运行时上下文注册。负责将 framework 层定义的单例上下文（`GatewayContext`、`FlowScriptContext`、`RepositoryHolderContext`）与 Spring 容器中的 Bean 进行绑定，启动延迟任务调度器。

## Maven 坐标

- groupId: com.codingapi.flow
- artifactId: flow-engine-starter
- version: 0.0.26
- packaging: jar

## 关联关系

> 以下关系由 `mvn dependency:tree` 指令结果生成，非人工推断。

### 我被哪些模块依赖

| 模块 | 依赖方式 |
|------|----------|
| flow-engine-example | 直接依赖 |

### 我依赖哪些模块

| 模块 | 说明 |
|------|------|
| flow-engine-framework | 核心框架层 |
| flow-engine-starter-infra | 持久化基础设施层 |
| flow-engine-starter-api | REST API 层 |
| flow-engine-starter-query | 查询层 |

### 主要外部依赖

无直接外部依赖，所有外部依赖由子模块传递。

## 项目结构

```
com.codingapi.flow/
├── AutoConfiguration.java               # 自动配置入口（注册 Bean + 上下文绑定）
├── register/                            # 上下文注册器（InitializingBean）
│   ├── GatewayContextRegister.java            # 注册 FlowOperatorGateway 到 GatewayContext 单例
│   ├── FlowScriptContextRegister.java         # 注册 IBeanFactory 到 FlowScriptContext 单例
│   └── RepositoryHolderContextRegister.java   # 注册服务/仓储到 RepositoryHolderContext 单例
├── runner/                              # 应用启动运行器
│   └── FlowDelayTaskRunner.java               # 延迟任务调度器（ApplicationRunner + DisposableBean）
└── web/
    └── dto/
        └── GroovyVariableMapping.java         # Groovy 变量映射 DTO
```

## 核心功能

### 1. 自动配置

`AutoConfiguration` 注册以下 Bean：

| Bean | 说明 |
|------|------|
| `FlowRecordService` | 流程记录服务（注入 3 个仓储） |
| `WorkflowService` | 工作流定义服务（注入 3 个仓储） |
| `FlowService` | 流程运行服务（注入 RepositoryHolderContext 单例） |
| `GatewayContextRegister` | 将 FlowOperatorGateway 绑定到 GatewayContext 单例 |
| `FlowScriptContextRegister` | 将 Spring ApplicationContext 包装为 IBeanFactory 绑定到 FlowScriptContext 单例 |
| `RepositoryHolderContextRegister` | 将服务和仓储绑定到 RepositoryHolderContext 单例 |
| `FlowDelayTaskRunner` | 应用启动时启动延迟任务调度器，关闭时销毁 |

关键类：`com.codingapi.flow.AutoConfiguration`

### 2. 上下文注册

三个注册器均实现 `InitializingBean`，在 Bean 初始化完成后将 Spring 管理的 Bean 注入 framework 层的全局单例，解决非 Spring 管理的领域对象（如反序列化恢复的 Workflow）需要访问 Spring Bean 的场景。

- `GatewayContextRegister` — 操作者网关注册
- `FlowScriptContextRegister` — 脚本运行时的 Bean 工厂注册（支持 Groovy 脚本中通过 `IBeanFactory` 获取 Spring Bean）
- `RepositoryHolderContextRegister` — 仓储和服务持有者注册

关键类：`com.codingapi.flow.register.*`

### 3. 延迟任务调度

`FlowDelayTaskRunner` 实现 `ApplicationRunner`，在应用启动时调用 `DelayTaskManager.getInstance().start()` 启动延迟任务调度；实现 `DisposableBean`，在应用关闭时调用 `close()` 清理资源。

关键类：`com.codingapi.flow.runner.FlowDelayTaskRunner`

## 对外 API

本模块不对外暴露 REST API，仅通过 Spring Boot 自动配置机制提供 Bean 注册和上下文绑定。

使用方只需引入本模块依赖，即可自动激活完整的流程引擎能力（持久化、API、查询）。

## 模块规范

- 所有注册器实现 `InitializingBean`，在 `afterPropertiesSet` 中完成单例绑定
- `FlowService` 通过 `RepositoryHolderContext` 单例创建，不直接注入 Spring Bean
- 自动配置通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册

## 构建指令

```bash
mvn -pl flow-engine-starter -am compile
mvn -pl flow-engine-starter test
mvn -pl flow-engine-starter package
```
