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
- **全局异常处理**：框架提供 `@ControllerAdvice` 统一处理异常能力（本项目未使用，异常处理走 codingapi 框架的 `LocaleMessageException` / `Response` 体系）

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

`flow-engine-starter-api` 模块使用 Spring Web 提供命令操作 REST API：
- `WorkflowController` — 流程设计 CRUD 相关 API（`/api/cmd/workflow`）
- `FlowRecordController` — 流程审批等命令操作 API（`/api/cmd/record`）

查询类 API 位于 `flow-engine-starter-query` 模块：
- `FlowRecordQueryController` — `/api/query/record`
- `WorkflowQueryController` — `/api/query/workflow`

## 使用实例

```java
@RestController
@RequestMapping("/api/cmd/workflow")
@AllArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowRepository workflowRepository;
    private final FlowOperatorGateway flowOperatorGateway;

    // 创建流程：无请求体，返回新建流程的 JSON
    @PostMapping("/create")
    public SingleResponse<JSONObject> create() {
        Workflow workflow = WorkflowBuilder.builder()
                .build(false);
        workflow.addDefaultNodesAndEdges();
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson());
        return SingleResponse.of(jsonObject);
    }

    // 保存流程
    @PostMapping("/save")
    public Response save(@RequestBody JSONObject request) {
        Workflow workflow = Workflow.formJson(request.toJSONString());
        workflow.updateTime();
        workflowService.saveWorkflow(workflow, true);
        return Response.buildSuccess();
    }

    // 加载流程
    @GetMapping("/load")
    public SingleResponse<JSONObject> load(String id) {
        Workflow workflow = workflowService.getWorkflowById(id);
        JSONObject jsonObject = JSONObject.parseObject(workflow.toJson());
        return SingleResponse.of(jsonObject);
    }
}
```