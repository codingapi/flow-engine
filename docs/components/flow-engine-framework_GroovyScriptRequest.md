---
module: flow-engine-framework
name: GroovyScriptRequest
description: Groovy 流程脚本中 request 参数的实际类型，封装流程会话中的节点、表单、操作人等上下文数据，供脚本读取和判断。
---

# GroovyScriptRequest

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

GroovyScriptRequest 是流程 Groovy 脚本中 `request` 参数的实际类型。在条件判断、人员加载、路由选择、触发执行、节点标题生成、子流程创建等所有脚本场景中，脚本通过 `request` 访问当前流程的节点信息、表单数据、操作人信息等上下文数据，以实现动态业务逻辑。

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
| `GroovyScriptRequest` | `com.codingapi.flow.script.request` | 脚本 request 对象，从 FlowSession 提取上下文数据 |
| `GroovyWorkflowRequest` | `com.codingapi.flow.script.request` | 工作流级别脚本请求对象，仅含 currentOperator 和 workflow |
| `FlowSession` | `com.codingapi.flow.session` | 流程会话，GroovyScriptRequest 的数据来源 |
| `IFlowOperator` | `com.codingapi.flow.operator` | 操作人接口，request 中暴露的操作人类型 |

### 关键方法

#### 流程信息

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getWorkflowTitle()` | `String` | 获取流程标题 |
| `getWorkflowId()` | `String` | 获取流程 ID |
| `getWorkflowCode()` | `String` | 获取流程编码 |

#### 节点信息

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getNodeName()` | `String` | 获取当前节点名称 |
| `getNodeType()` | `String` | 获取当前节点类型 |
| `getCurrentNode()` | `IFlowNode` | 获取当前节点对象 |
| `getStartNode()` | `IFlowNode` | 获取开始节点 |
| `getNode(String nodeId)` | `IFlowNode` | 根据 ID 获取节点 |
| `getCurrentAction()` | `IFlowAction` | 获取当前动作 |

#### 表单数据

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getFormData()` | `Map<String, Object>` | 获取表单字段值（完整 Map） |
| `getFormData(String fieldCode)` | `Object` | 获取指定字段的值 |
| `getSubFormData(String subFormCode)` | `List<Map<String, Object>>` | 获取子表单数据列表 |

#### 操作人信息

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getCreatedOperator()` | `IFlowOperator` | 获取流程创建者 |
| `getCreatedOperatorId()` | `long` | 获取流程创建者 ID |
| `getCreatedOperatorName()` | `String` | 获取流程创建者名称 |
| `getCurrentOperator()` | `IFlowOperator` | 获取当前审批人 |
| `getCurrentOperatorId()` | `long` | 获取当前审批人 ID |
| `getCurrentOperatorName()` | `String` | 获取当前审批人名称 |
| `getSubmitOperator()` | `IFlowOperator` | 获取流程提交人 |
| `getSubmitOperatorId()` | `long` | 获取流程提交人 ID |
| `getSubmitOperatorName()` | `String` | 获取流程提交人名称 |
| `isFlowManager()` | `boolean` | 当前审批人是否流程管理员 |

#### 子流程与请求创建

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `toCreateRequest()` | `FlowCreateRequest` | 转换为当前流程的创建请求（用于子流程） |
| `toCreateRequest(String, long, String, String)` | `FlowCreateRequest` | 指定 workId、operatorId、actionId、formData 创建请求 |
| `toCreateRequest(String, long, String, Map)` | `FlowCreateRequest` | 指定 workId、operatorId、actionId、formData(Map) 创建请求 |

#### 其他

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `isMock()` | `boolean` | 是否模拟测试环境 |

### 构造方式

GroovyScriptRequest 由各脚本节点类型在执行时从 FlowSession 自动构造，脚本开发者不直接实例化：

```java
// 各 Script 节点中的标准构造方式
GroovyScriptRequest request = new GroovyScriptRequest(session);
```

### 使用场景

GroovyScriptRequest 在以下 8 种脚本节点类型中作为 `request` 参数传入：

| 脚本节点 | 类名 | 用途 |
|----------|------|------|
| 条件脚本 | `ConditionScript` | 判断条件是否满足 |
| 人员加载脚本 | `OperatorLoadScript` | 动态加载下一节点审批人 |
| 人员匹配脚本 | `OperatorMatchScript` | 匹配当前操作人是否合法 |
| 路由脚本 | `RouterNodeScript` | 决定流程走向 |
| 触发脚本 | `TriggerScript` | 执行触发动作 |
| 异常触发脚本 | `ErrorTriggerScript` | 处理异常情况 |
| 节点标题脚本 | `NodeTitleScript` | 动态生成节点标题 |
| 子流程脚本 | `SubProcessScript` | 创建子流程 |

### 配置项

GroovyScriptRequest 自身无配置项。

## 使用示例

### 条件判断脚本

```groovy
// 根据表单金额判断是否需要高级审批
def run(request){
    def amount = request.getFormData('amount')
    return amount > 10000
}
```

### 人员加载脚本

```groovy
// 加载流程创建者为审批人
def run(request){
    return [request.getCreatedOperatorId()]
}
```

### 路由脚本

```groovy
// 根据表单类型路由到不同节点
def run(request){
    def type = request.getFormData('type')
    if(type == 'urgent'){
        return request.getNode('node_urgent').getId()
    }
    return request.getStartNode().getId()
}
```

### 节点标题脚本

```groovy
// 动态生成待办标题
def run(request){
    def applicant = request.getCreatedOperatorName()
    return applicant + '提交的审批申请'
}
```

### 触发脚本

```groovy
// 触发时打印日志
def run(request){
    def title = request.getWorkflowTitle()
    print('流程触发: ' + title + '\n')
}
```

### 子流程创建脚本

```groovy
// 创建子流程
def run(request){
    return request.toCreateRequest()
}
```

## 注意事项

- **不可直接构造**：GroovyScriptRequest 由各脚本节点类型从 FlowSession 自动创建，脚本开发者只需在脚本中使用 `request` 变量
- **数据快照**：构造时从 FlowSession 提取一次数据（操作人、节点、表单等），后续对 FlowSession 的修改不会反映到已创建的 request 对象
- **request 变量名**：在 Groovy 脚本中固定以 `run(request)` 的参数形式接收，变量名由脚本约定
- **GroovyWorkflowRequest 区别**：`GroovyWorkflowRequest` 是更轻量的请求对象（仅含 currentOperator 和 workflow），用于工作流级别的脚本场景
- **表单数据类型**：`getFormData(String)` 返回 Object，脚本中需自行处理类型转换；子表单返回 `List<Map<String, Object>>`
