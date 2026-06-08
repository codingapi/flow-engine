---
name: springboot/cache
module: springboot
description: Spring Cache 声明式缓存能力，通过注解简化缓存操作
status: 已实现
scope: 后端
source: 框架:Spring Boot
import: "org.springframework.boot:spring-boot-starter-cache"
framework_version: "3.5.9"
---

## 解决什么问题

提供 Spring 的声明式缓存能力，解决以下问题：

- **透明缓存**：通过注解在方法级别自动缓存和失效，无需手动管理缓存逻辑
- **缓存抽象**：统一缓存 API，支持多种缓存实现（ConcurrentMapCache、Redis、Caffeine 等）
- **缓存策略**：支持条件缓存、Key 自定义、多缓存名称等高级策略

## 如何使用

### 核心注解

| 注解 | 用途 |
|------|------|
| `@EnableCaching` | 启用缓存支持 |
| `@Cacheable` | 方法结果缓存 |
| `@CacheEvict` | 缓存失效 |
| `@CachePut` | 更新缓存 |
| `@Caching` | 组合多个缓存操作 |

### 在 Flow Engine 中的使用

Flow Engine 的 `WorkflowRuntimeCache` 使用内存缓存机制缓存运行时流程配置，避免频繁的数据库查询和 JSON 反序列化。

## 使用实例

```java
// 启用缓存
@Configuration
@EnableCaching
public class CacheConfig {
}

// 方法级缓存
@Service
public class WorkflowService {

    @Cacheable(value = "workflow", key = "#code")
    public Workflow getByCode(String code) {
        return workflowRepository.findByCode(code);
    }

    @CacheEvict(value = "workflow", key = "#workflow.code")
    public void saveWorkflow(Workflow workflow) {
        workflowRepository.save(workflow);
    }
}
```
