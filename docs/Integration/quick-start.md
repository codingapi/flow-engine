# 快速开始

以 `flow-engine-example` 模块为范本，介绍在业务项目中集成 Flow Engine 的最小步骤。

## 1. 引入依赖

在业务项目的 `pom.xml` 中声明：

```xml
<dependencies>
    <!-- 引擎核心 + Spring Boot 自动配置 -->
    <dependency>
        <groupId>com.codingapi.flow</groupId>
        <artifactId>flow-engine-starter</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

    <!-- JPA 仓储实现（提供 11 张 t_flow_* 表与仓储 Bean） -->
    <dependency>
        <groupId>com.codingapi.flow</groupId>
        <artifactId>flow-engine-starter-infra</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

    <!-- 命令 REST API（可选） -->
    <dependency>
        <groupId>com.codingapi.flow</groupId>
        <artifactId>flow-engine-starter-api</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

    <!-- 查询 REST API（可选） -->
    <dependency>
        <groupId>com.codingapi.flow</groupId>
        <artifactId>flow-engine-starter-query</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

> 依赖 `flow-engine-starter` 后，`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 与 `META-INF/spring.factories` 会自动注册 `com.codingapi.flow.AutoConfiguration`，无需手动配置。

## 2. 提供必选 Bean

引擎自动配置强依赖以下 Bean，缺失即启动失败：

| Bean | 参考实现 |
|------|----------|
| `FlowOperatorGateway` | 见 [流程用户体系](./user-integration.md) |
| `NodeViewJavaScriptRepository` | 见 [仓储抽象](./repository-integration.md) |
| `GroovyScriptRepository`、`TempGroovyScriptRepository` | 见 [仓储抽象](./repository-integration.md) |

完整清单见 [Spring Boot 自动配置](./auto-configuration.md)。

## 3. 编写一个最简单的流程

使用 `WorkflowBuilder` 编程式构建，并保存到仓储：

```java
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.node.nodes.ApprovalNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;

// 1. 定义表单
FlowForm form = FlowFormBuilder.builder()
        .name("请假申请单")
        .code("leave_form")
        .addField("请假天数", "days", DataType.INTEGER)
        .addField("请假原因", "reason", DataType.STRING)
        .build();

// 2. 定义节点
StartNode startNode = StartNode.defaultNode();
ApprovalNode approvalNode = ApprovalNode.defaultNode();   // 默认操作人 = 流程创建者
EndNode endNode = EndNode.defaultNode();

// 3. 组装流程（开始 → 审批 → 结束）
Workflow workflow = WorkflowBuilder.builder()
        .title("请假审批")
        .code("leave_flow")
        .form(form)
        .createdOperator(currentOperator)
        .addNode(startNode)
        .addNode(approvalNode)
        .addNode(endNode)
        .build();   // build() 默认 enable + verify
```

> 节点连接不依赖显式 edge：引擎按 `order` 顺序 + 分支节点语义自动推导流转（见 [节点/动作/策略扩展](./extension-points.md)）。

## 4. 保存流程

```java
@Resource
private WorkflowService workflowService;

workflowService.saveWorkflow(workflow);   // 保存流程设计（含脚本固化）
```

## 5. 发起流程

```java
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.service.FlowService;

@Resource
private FlowService flowService;

FlowCreateRequest request = new FlowCreateRequest();
request.setWorkCode("leave_flow");
request.setOperatorId(1001L);
request.setActionId("...");                       // 开始节点的 SAVE 动作 id
request.setFormData(Map.of("days", 3, "reason", "年假"));

long recordId = flowService.create(request);      // 返回首条流程记录 id
```

## 6. 发起后发生了什么

`FlowService.create` 内部经 `FlowCreateService` 完成（对应 `FlowCreateService.create()`）：

1. 校验请求参数；
2. 按 `workCode` 加载流程，校验启用状态与创建者匹配（`OperatorMatchScript`）；
3. `getOrCreateWorkflowRuntime(workflow)` 生成/复用**运行时快照**（`WorkflowRuntime`），固化流程 JSON 与全部脚本内容 —— 后续改设计不影响已运行流程；
4. 把脚本快照写入 `FlowRuntimeScriptLocalCache`（线程级）；
5. 构建 `FlowSession`，`StartNode.generateCurrentRecords` 生成首条 `FlowRecord`；
6. 落库（`repositoryHolder.saveRecords`）；
7. 推送 `FlowRecordStartEvent` + `FlowRecordTodoEvent`（异步）。

## 7. 审批流转

```java
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.response.ActionResponse;

FlowActionRequest actionRequest = new FlowActionRequest();
actionRequest.setRecordId(recordId);
actionRequest.setOperatorId(1001L);
// advice: actionId + advice 文本 + 可选 forwardOperators/backNodeId 等
ActionResponse response = flowService.action(actionRequest);
```

审批通过后引擎自动推进到下一节点并生成新的待办记录，同时推送 `FlowRecordDoneEvent`（当前记录）与 `FlowRecordTodoEvent`（新待办）。

## 下一步

- 对接自己的用户体系 → [流程用户体系](./user-integration.md)
- 定制流程默认脚本 → [流程默认脚本](./script-integration.md)
- 订阅流程事件（通知/消息）→ [流程事件机制](./event-integration.md)
- 不引入 JPA infra，自行实现存储 → [仓储抽象与持久化](./repository-integration.md)
- 使用 REST API 调用 → [REST API](./rest-api.md)