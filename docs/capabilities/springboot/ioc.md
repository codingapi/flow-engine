---
name: springboot/ioc
module: springboot
description: Spring Framework IoC 容器、依赖注入、AOP 和事件发布
status: 已实现
scope: 后端
source: 框架:Spring Boot
import: "org.springframework.boot:spring-boot-starter"
framework_version: "3.5.9"
---

## 解决什么问题

提供 Spring Boot 的 IoC（控制反转）容器能力，解决以下问题：

- **依赖注入**：容器管理 Bean 之间的依赖关系。本项目不使用 `@Autowired` / `@Inject` 注解，统一通过 Lombok `@AllArgsConstructor` 构造器注入（如 `WorkflowController`、`WorkflowService`、各 `RepositoryImpl`）
- **Bean 生命周期管理**：容器统一管理 Bean 的创建、初始化和销毁
- **AOP 支持**：框架提供 `@Aspect` 横切关注点能力（本项目未使用）
- **事件发布**：本项目不使用 Spring `ApplicationEventPublisher`，事件系统为自研 `IFlowEvent` + `EventPusher`（`flow-engine-framework/event/` 下 7 种事件类，操作完成后通过 `EventPusher.push(event)` 推送）
- **自动配置**：通过 `@Configuration` + `@Conditional` 实现条件化 Bean 注册

## 如何使用

### 核心注解

| 注解 | 用途 |
|------|------|
| `@Component` / `@Service` / `@Repository` | 声明 Bean |
| `@Autowired` | 自动注入依赖（本项目未使用，统一构造器注入） |
| `@Configuration` | 配置类 |
| `@Bean` | 方法级 Bean 声明 |
| `@ConditionalOnClass` / `@ConditionalOnMissingBean` | 条件化配置 |
| `@Transactional` | 声明式事务 |

### 在 Flow Engine 中的使用

Flow Engine 通过 `AutoConfiguration` 类使用 Spring IoC：
- `flow-engine-starter` 注册核心服务 Bean（`FlowService`、`WorkflowService`）
- `flow-engine-starter-infra` 注册 JPA Repository 实现（`@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")`）
- `flow-engine-starter-api` 注册 REST Controller

## 使用实例

```java
// 自动配置类（flow-engine-starter）
@Configuration
public class AutoConfiguration {

    @Bean
    public WorkflowService workflowService(WorkflowVersionRepository workflowVersionRepository,
                                           WorkflowRepository workflowRepository,
                                           WorkflowRuntimeRepository workflowRuntimeRepository) {
        return new WorkflowService(workflowVersionRepository, workflowRepository, workflowRuntimeRepository);
    }

    @Bean
    public FlowService flowService(RepositoryHolderContextRegister repositoryHolderContextRegister) {
        return new FlowService(RepositoryHolderContext.getInstance());
    }
}
```

```java
// 使用事务
@Transactional
public class FlowService {
    public long create(FlowCreateRequest request) {
        // 在事务内执行
    }
}
```