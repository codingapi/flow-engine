# Flow Engine 集成手册（后端）

> 面向业务项目的后端集成指南。本文档以仓库实际代码为依据，介绍 Flow Engine 工作流引擎允许下游（业务方）集成与扩展的全部能力，包括：**流程用户体系、流程默认脚本、流程事件机制、仓储抽象与持久化、Spring Boot 自动配置、REST API、Mock 模式、节点/动作/策略配置**等。

## 模块结构

| 模块 | 说明 | 集成角色 |
|------|------|----------|
| `flow-engine-framework` | 核心引擎：节点/动作/策略/脚本/服务/仓储接口（Spring 无关的领域核心） | 只读，一般不改动 |
| `flow-engine-starter` | Spring Boot 自动配置入口，把引擎单例与 Spring 容器打通 | 直接依赖 |
| `flow-engine-starter-infra` | JPA 持久化实现（11 张 `t_flow_*` 表 + 11 个仓储实现） | 可直接依赖；也可自行实现仓储接口替换 |
| `flow-engine-starter-api` | 命令 REST API（发起/审批/撤销/催办/删除 + 流程设计 CRUD） | 可选依赖 |
| `flow-engine-starter-query` | 查询 REST API（待办/已办/抄送/全部列表） | 可选依赖 |
| `flow-engine-example` | 下游集成范本（用户体系、事件订阅、脚本仓储、安全） | 参考实现 |

## 下游必须提供的能力（概览）

集成方**必须**在自身应用中提供以下 Spring Bean / 实现，否则引擎自动配置启动失败：

| # | 必须提供 | 说明 | 对应文档 |
|---|----------|------|----------|
| 1 | `FlowOperatorGateway` | 对接应用用户体系的防腐层（`get(id)` / `findByIds(ids)`） | [流程用户体系](./user-integration.md) |
| 2 | `IFlowOperator` 实现类 | 用户实体/DTO，实现 `getUserId/getName/isFlowManager/forwardOperator` | [流程用户体系](./user-integration.md) |
| 3 | `NodeViewJavaScriptRepository` | 节点视图 JS 持久化（infra 不提供，需自建） | [仓储抽象](./repository-integration.md) |
| 4 | 脚本持久化：`GroovyScriptRepository`、`TempGroovyScriptRepository` | Groovy 脚本落库（外部 starter-script 的 SPI） | [仓储抽象](./repository-integration.md) |
| 5 | 11 个仓储接口（仅当不引入 `flow-engine-starter-infra` 时） | 自行实现存储 | [仓储抽象](./repository-integration.md) |

## 文档目录

| 文档 | 内容 |
|------|------|
| [快速开始](./quick-start.md) | 最小集成步骤：引入依赖 → 提供 Bean → 编写流程 → 发起流程 |
| [流程用户体系](./user-integration.md) | `FlowOperatorGateway` / `IFlowOperator` / `GatewayContext` / 线程缓存 / 当前登录人 |
| [流程默认脚本](./script-integration.md) | 12 种脚本类型、`GroovyScriptRequest` 完整 API、`$bind`、默认脚本替换、脚本生命周期 |
| [流程事件机制](./event-integration.md) | 7 种事件、`EventPusher`、`IHandler` 订阅、异步分发管道、事务变体、推送时机 |
| [仓储抽象与持久化](./repository-integration.md) | `IRepositoryHolder`、12 个仓储接口（含 11 个 JPA 实现 + 需自行实现的 `NodeViewJavaScriptRepository`）、脚本仓储、锁机制 |
| [Spring Boot 自动配置](./auto-configuration.md) | `AutoConfiguration` 8 个 Bean、4 个 Register、必选/可选 Bean 清单 |
| [REST API](./rest-api.md) | 4 个 Controller、`mockKey`/`operatorId` 分流机制、请求/响应结构 |
| [Mock 模式](./mock-mode.md) | 全内存沙箱 `MockInstance`、15 分钟过期、接入方式 |
| [节点/动作/策略扩展](./extension-points.md) | 19 种节点 × 8 种动作 × 15 种节点策略 × 2 种流程策略，扩展方式 |
| [编程式 API](./programmatic-api.md) | `WorkflowBuilder`/`FlowFormBuilder` 构建流程、`FlowService` 服务编排、查询服务 |

## 核心架构速览

### 单例上下文家族（框架层，无 Spring 注解）

引擎核心以「静态单例 + Setter 注入」模式运作，Spring 集成由 starter 的 `*Register`（`InitializingBean`）在启动时把 Bean 「灌入」单例：

| 单例 | 作用 | 注入来源 |
|------|------|----------|
| `GatewayContext` | 操作人网关持有者（带线程缓存） | `GatewayContextRegister` |
| `RepositoryHolderContext` | 资源持有者（7 个仓储/服务） | `RepositoryHolderContextRegister` |
| `FlowScriptContext` | 脚本 `$bind` 上下文（`IBeanFactory`） | `FlowScriptContextRegister` |
| `FlowIDGeneratorGatewayContext` | ID 生成器（可替换扩展点） | 默认内建，可选替换 |
| `NodeViewJavaScriptCacheContext` | 节点视图 JS 缓存（15 分钟） | `NodeViewJavaScriptCacheContextRegister` |
| `FlowOperatorLocalThreadCache` | 操作人线程缓存 | 引擎内部使用 |
| `FlowRuntimeScriptLocalCache` | 脚本快照线程缓存 | 引擎内部使用 |
| `MockInstanceFactory` | Mock 沙箱工厂 | api 模块调用 |

### 执行链路

```
REST 请求 → FlowRecordController
  （mockKey 分流 Mock / 生产；operatorId 覆盖当前用户）
  → FlowService（@Transactional，清线程缓存）
    → FlowXxxService（create / action / revoke / delete / urge / detail）
      → FlowSession（不可变会话）→ 节点 handle → 动作 run → 策略执行
      → repositoryHolder.saveRecords（落库）
      → EventPusher.push（推送事件，异步）
```

## 版本约定

- 引擎版本：`0.1.0-SNAPSHOT`（根 pom `revision`）
- 外部框架依赖（根 pom `codingapi.framework.version = 17.3.0`）：
  - `com.codingapi.springboot:springboot-starter` —— 事件体系（`EventPusher`/`IHandler`）、`UserContext`、`LocaleMessageException`
  - `com.codingapi.springboot:springboot-starter-script` —— Groovy 脚本引擎（`GroovyScript`/注解/仓储 SPI）
  - `com.codingapi.springboot:springboot-starter-data-fast` —— `FastRepository`（JPA 仓储基类）
- Java 17、Spring Boot 3.5.9