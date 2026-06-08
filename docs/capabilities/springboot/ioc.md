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

- **依赖注入**：通过 `@Autowired` / `@Inject` 实现组件间的松耦合
- **Bean 生命周期管理**：容器统一管理 Bean 的创建、初始化和销毁
- **AOP 支持**：通过 `@Aspect` 实现横切关注点（事务、日志、权限等）
- **事件发布**：通过 `ApplicationEventPublisher` 实现组件间的事件通知
- **自动配置**：通过 `@Configuration` + `@Conditional` 实现条件化 Bean 注册

## 如何使用

### 核心注解

| 注解 | 用途 |
|------|------|
| `@Component` / `@Service` / `@Repository` | 声明 Bean |
| `@Autowired` | 自动注入依赖 |
| `@Configuration` | 配置类 |
| `@Bean` | 方法级 Bean 声明 |
| `@ConditionalOnClass` / `@ConditionalOnMissingBean` | 条件化配置 |
| `@Transactional` | 声明式事务 |

### 在 Flow Engine 中的使用

Flow Engine 通过 `AutoConfiguration` 类使用 Spring IoC：
- `flow-engine-starter` 注册核心服务 Bean（`FlowService`、`WorkflowService`）
- `flow-engine-starter-infra` 注册 JPA Repository 实现
- `flow-engine-starter-api` 注册 REST Controller

## 使用实例

```java
// 自动配置类
@Configuration
@ConditionalOnClass(FlowService.class)
public class AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FlowService flowService(IRepositoryHolder holder) {
        return new FlowService(holder);
    }

    @Bean
    public WorkflowService workflowService(WorkflowRepository repo) {
        return new WorkflowService(repo);
    }
}

// 使用事务
@Transactional
public class FlowService {
    public long create(FlowCreateRequest request) {
        // 在事务内执行
    }
}
```
