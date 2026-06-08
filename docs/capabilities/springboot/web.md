---
name: springboot/web
module: springboot
description: Spring Boot Web 能力，提供 REST API、Controller 和请求处理
status: 已实现
scope: 后端
source: 框架:Spring Boot
import: "org.springframework.boot:spring-boot-starter-web"
framework_version: "3.5.9"
---

## 解决什么问题

提供 Spring Boot 的 Web 服务能力，解决以下问题：

- **REST API**：通过 `@RestController` 声明 REST 端点
- **请求映射**：通过 `@RequestMapping` / `@GetMapping` / `@PostMapping` 映射 HTTP 请求
- **参数绑定**：通过 `@RequestBody` / `@RequestParam` / `@PathVariable` 绑定请求参数
- **响应序列化**：自动将返回对象序列化为 JSON
- **全局异常处理**：通过 `@ControllerAdvice` 统一处理异常

## 如何使用

### 核心注解

| 注解 | 用途 |
|------|------|
| `@RestController` | 声明 REST Controller |
| `@RequestMapping` | 请求路径映射 |
| `@GetMapping` / `@PostMapping` | HTTP 方法映射 |
| `@RequestBody` | 请求体绑定 |
| `@PathVariable` | 路径变量绑定 |

### 在 Flow Engine 中的使用

`flow-engine-starter-api` 模块使用 Spring Web 提供 REST API：
- `WorkflowController` — 流程设计相关 API
- 流程审批、查询等操作 API

## 使用实例

```java
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/create")
    public ResponseEntity<Long> create(@RequestBody WorkflowCreateRequest request) {
        long id = workflowService.saveWorkflow(request);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDetail> detail(@PathVariable long id) {
        return ResponseEntity.ok(workflowService.getDetail(id));
    }
}
```
