---
name: flow-engine-starter/auto-configuration
module: flow-engine-starter
description: Spring Boot 自动配置入口，注册流程引擎的核心组件和仓储上下文
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-starter"
symbols:
  - AutoConfiguration
content_hash: cead1cab34f0a1357e650ae764b4f008247c530ad9b4eff0cd0ffeb9c93d44e3
---

## 解决什么问题

提供流程引擎的 Spring Boot 自动配置能力，解决以下问题：

- **零配置启动**：引入 `flow-engine-starter` 依赖后自动注册流程引擎的核心 Bean
- **模块化配置**：四个模块各自有独立的 `AutoConfiguration`，职责清晰
- **仓储上下文注册**：自动注册 `RepositoryHolderContext`，将各 Repository 实现注入到框架层

## 如何使用

### 引入依赖

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-starter</artifactId>
    <version>${flow-engine.version}</version>
</dependency>
```

### 模块自动配置分工

| 模块 | AutoConfiguration 职责 |
|------|----------------------|
| `flow-engine-starter` | 注册 `WorkflowService`、`FlowService`、`RepositoryHolderContext` |
| `flow-engine-starter-api` | 注册 REST Controller（`WorkflowController` 等） |
| `flow-engine-starter-infra` | 注册 JPA Repository 实现、数据库实体 |
| `flow-engine-starter-query` | 注册查询服务（`FlowRecordQueryService` 等） |

### 注册 GatewayContext

在 `flow-engine-starter` 的 `AutoConfiguration` 中，通过 `GatewayContextRegister` 将业务实现的 `FlowOperatorGateway` 注入到 `GatewayContext` 单例。

### 业务扩展

业务项目只需实现以下接口即可接入流程引擎：
- `FlowOperatorGateway` — 提供操作者信息查询
- 自定义 Controller（如需覆盖默认 API）

## 使用实例

```java
// 1. pom.xml 引入 starter
// <dependency>
//     <groupId>com.codingapi.flow</groupId>
//     <artifactId>flow-engine-starter</artifactId>
// </dependency>

// 2. 实现 FlowOperatorGateway
@Component
public class MyFlowOperatorGateway implements FlowOperatorGateway {
    @Override
    public IFlowOperator get(long userId) { /* ... */ }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) { /* ... */ }
}

// 3. 自动注入 FlowService
@Autowired
private FlowService flowService;

// 4. 使用流程引擎
long flowId = flowService.create(createRequest);
flowService.action(actionRequest);
```
