# GroovyWorkflowRequest

## 何时使用

当需要在流程定义级别（而非流程实例级别）的 Groovy 脚本中访问操作人和流程定义信息时使用。`GroovyWorkflowRequest` 是轻量级的脚本请求对象，仅携带当前操作人和流程定义，用于操作者匹配脚本（`OperatorMatchScript`）等不需要完整流程会话上下文的场景。

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.27</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `GroovyWorkflowRequest` | `com.codingapi.flow.script.request` | 轻量级流程级脚本请求对象 |
| `OperatorMatchScript` | `com.codingapi.flow.script.node` | 操作者匹配脚本，消费 `GroovyWorkflowRequest` |

### 关键方法

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getCurrentOperator()` | 无 | `IFlowOperator` | 获取当前操作人 |
| `getWorkflow()` | 无 | `Workflow` | 获取流程定义 |

### 与 GroovyScriptRequest 的对比

| 特性 | `GroovyScriptRequest` | `GroovyWorkflowRequest` |
|------|----------------------|------------------------|
| 上下文级别 | 流程实例级（`FlowSession`） | 流程定义级 |
| 表单数据 | 有 | 无 |
| 节点信息 | 有 | 无 |
| 操作人信息 | 创建人、审批人、提交人 | 仅当前操作人 |
| 流程信息 | 实例 ID、标题、编码 | 流程定义对象 |
| 子流程创建 | 支持 | 不支持 |
| 使用场景 | 条件、路由、触发器等节点脚本 | 操作者匹配脚本 |

## 使用示例

```groovy
// 操作者匹配脚本（OperatorMatchScript）
// 判断当前操作人是否匹配流程创建者
def run(request) {
    def operator = request.getCurrentOperator()
    def workflow = request.getWorkflow()
    return operator.getUserId() == workflow.getCreatedOperatorId()
}
```

```groovy
// 任意操作者匹配（默认脚本）
def run(request) {
    return true
}
```

```java
// Java 中构建请求（Workflow.matchCreatedOperator 内部调用）
GroovyWorkflowRequest request = new GroovyWorkflowRequest(flowOperator, workflow);
boolean matched = operatorCreateScript.execute(request);
```

```java
// 单元测试
IFlowOperator operator = new TestOperator(1, "lorne");
OperatorMatchScript script = OperatorMatchScript.any();
GroovyWorkflowRequest request = new GroovyWorkflowRequest(operator, null);
assertTrue(script.execute(request));
```

## 注意事项

- **轻量级设计**：仅包含 `currentOperator` 和 `workflow` 两个字段，不含表单数据、节点信息等实例级上下文
- **构造方式**：通过全参构造器创建，框架内部由 `Workflow.matchCreatedOperator()` 自动构建
- **`workflow` 可为 null**：在测试场景中 `workflow` 可传 null，脚本中需注意空判断
- **脚本返回值**：操作者匹配脚本期望返回 `Boolean`（`true` 表示匹配，`false` 表示不匹配）
- **与 `GroovyScriptRequest` 互不替代**：两者服务于不同粒度的脚本场景，`GroovyWorkflowRequest` 用于流程定义级别，`GroovyScriptRequest` 用于流程实例级别
