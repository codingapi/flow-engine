---
module: flow-engine-framework
name: GroovyWorkflowRequest
description: 工作流级别的轻量级脚本请求对象，仅包含当前操作人和工作流信息，用于人员匹配脚本和流程创建者验证场景。
---

# GroovyWorkflowRequest

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

GroovyWorkflowRequest 用于工作流级别（非会话级别）的脚本场景。与 GroovyScriptRequest 提供完整的会话上下文不同，GroovyWorkflowRequest 仅携带当前操作人和工作流对象，适用于人员匹配脚本（OperatorMatchScript）中判断当前操作人是否满足条件，以及工作流创建者验证等轻量级判断场景。

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.30</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `GroovyWorkflowRequest` | `com.codingapi.flow.script.request` | 工作流级别脚本请求对象，仅含操作人和工作流 |
| `OperatorMatchScript` | `com.codingapi.flow.script.node` | 人员匹配脚本，使用 GroovyWorkflowRequest 作为请求参数 |
| `Workflow` | `com.codingapi.flow.workflow` | 工作流定义，GroovyWorkflowRequest 的数据来源之一 |

### 关键方法

#### GroovyWorkflowRequest（通过构造器创建）

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getCurrentOperator()` | `IFlowOperator` | 获取当前操作人 |
| `getWorkflow()` | `Workflow` | 获取工作流定义对象 |

### 构造方式

通过全参构造器创建，通常由框架内部在以下场景构造：

```java
// Workflow.matchCreatedOperator() 中
GroovyWorkflowRequest request = new GroovyWorkflowRequest(flowOperator, this);

// OperatorMatchScript.execute() 中接收已构造的请求
```

### 使用场景

| 场景 | 调用方 | 用途 |
|------|--------|------|
| 人员匹配脚本 | `OperatorMatchScript.execute()` | 判断当前操作人是否匹配脚本条件 |
| 工作流创建者验证 | `Workflow.matchCreatedOperator()` | 验证指定操作人是否为工作流创建者 |

### 与 GroovyScriptRequest 的区别

| 特性 | GroovyWorkflowRequest | GroovyScriptRequest |
|------|----------------------|---------------------|
| 数据来源 | 直接传入操作人和工作流 | 从 FlowSession 提取 |
| 包含信息 | 操作人 + 工作流 | 节点、表单、操作人、动作等完整上下文 |
| 使用场景 | 人员匹配、创建者验证 | 条件判断、路由、触发、标题生成等 |
| 使用节点 | OperatorMatchScript | 8 种脚本节点类型 |

### 配置项

GroovyWorkflowRequest 自身无配置项。

## 使用示例

### 人员匹配脚本（默认任意人）

```groovy
// 默认人员匹配脚本 - 允许任意用户
def run(request){
    return true
}
```

### 人员匹配脚本（限定条件）

```groovy
// 仅允许工作流创建者操作
def run(request){
    def creator = request.getWorkflow().getCreatedOperator()
    return request.getCurrentOperator().getUserId() == creator.getUserId()
}
```

### 测试中的用法

```java
@Test
void execute() {
    IFlowOperator flowOperator = new User(1, "lorne");
    OperatorMatchScript operatorMatchScript = OperatorMatchScript.any();
    assertTrue(operatorMatchScript.execute(
        new GroovyWorkflowRequest(flowOperator, null)
    ));
}
```

## 注意事项

- **轻量级**：相比 GroovyScriptRequest，GroovyWorkflowRequest 仅包含操作人和工作流两个字段，不提供表单数据、节点信息等会话级上下文
- **workflow 可为 null**：在测试场景或部分调用中 workflow 参数可能为 null，脚本中访问 workflow 属性时需做空值判断
- **不可直接用于会话级脚本**：条件脚本、路由脚本、触发脚本等会话级场景应使用 GroovyScriptRequest
- **构造简洁**：使用 `@AllArgsConstructor` 的全参构造器，字段均为 final 不可变
