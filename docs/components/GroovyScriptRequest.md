# GroovyScriptRequest

## 何时使用

当编写流程引擎的 Groovy 脚本时，`GroovyScriptRequest` 作为脚本的 `request` 参数注入，提供对流程上下文的完整访问能力。所有 Groovy 脚本的入口签名为 `def run(request){ ... }`，其中 `request` 即为此类实例。典型场景：

- 条件分支脚本中读取表单数据判断走哪个分支
- 路由脚本中根据表单值决定目标节点
- 操作者加载脚本中根据创建人或表单数据动态计算审批人
- 子流程脚本中构建子流程创建请求
- 节点标题脚本中动态生成待办标题

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.28</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `GroovyScriptRequest` | `com.codingapi.flow.script.request` | 脚本请求对象，绑定到 Groovy 脚本的 `request` 参数 |
| `GroovyScriptBind` | `com.codingapi.flow.script.request` | 脚本绑定对象，绑定到 Groovy 脚本的 `$bind` 参数 |
| `GroovyWorkflowRequest` | `com.codingapi.flow.script.request` | 轻量级流程级请求对象（仅含操作人和流程定义） |

### 关键方法

#### 流程元数据

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getWorkflowTitle()` | `String` | 流程标题 |
| `getWorkflowId()` | `String` | 流程实例 ID |
| `getWorkflowCode()` | `String` | 流程编码 |

#### 节点信息

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getNodeName()` | 无 | `String` | 当前节点名称 |
| `getNodeType()` | 无 | `String` | 当前节点类型 |
| `getNode(String)` | `nodeId` — 节点 ID | `IFlowNode` | 根据 ID 获取节点信息 |
| `getStartNode()` | 无 | `IFlowNode` | 获取开始节点 |

#### 表单数据

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getFormData()` | 无 | `Map<String, Object>` | 获取全部表单字段值 |
| `getFormData(String)` | `fieldCode` — 字段编码 | `Object` | 获取指定表单字段值 |
| `getSubFormData(String)` | `subFormCode` — 子表单编码 | `List<Map<String, Object>>` | 获取子表单数据列表 |

#### 操作人信息

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `getCreatedOperator()` | `IFlowOperator` | 流程创建人 |
| `getCreatedOperatorId()` | `long` | 流程创建人 ID |
| `getCreatedOperatorName()` | `String` | 流程创建人名称 |
| `getCurrentOperator()` | `IFlowOperator` | 当前审批人 |
| `getCurrentOperatorId()` | `long` | 当前审批人 ID |
| `getCurrentOperatorName()` | `String` | 当前审批人名称 |
| `getSubmitOperator()` | `IFlowOperator` | 流程提交人（可能为 null） |
| `getSubmitOperatorId()` | `long` | 流程提交人 ID（未设置返回 0） |
| `getSubmitOperatorName()` | `String` | 流程提交人名称（未设置返回 null） |

#### 权限与状态

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `isFlowManager()` | `boolean` | 当前操作人是否流程管理员 |
| `isMock()` | `boolean` | 是否模拟测试模式 |

#### 子流程创建

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `toCreateRequest()` | 无 | `FlowCreateRequest` | 转换为当前流程的创建请求 |
| `toCreateRequest(String, long, String, String)` | `workId` — 流程设计 ID, `operatorId` — 操作人 ID, `actionId` — 动作类型, `formData` — 表单数据(JSON) | `FlowCreateRequest` | 创建子流程请求 |
| `toCreateRequest(String, long, String, Map)` | `workId`, `operatorId`, `actionId`, `formData`（Map） | `FlowCreateRequest` | 创建子流程请求（Map 表单） |

## 使用示例

```groovy
// 条件分支脚本：根据表单金额判断
def run(request) {
    def amount = request.getFormData("amount")
    if (amount != null && Double.parseDouble(amount.toString()) > 10000) {
        return true  // 走大额审批分支
    }
    return false
}
```

```groovy
// 路由脚本：根据部门决定目标节点
def run(request) {
    def dept = request.getFormData("department")
    if (dept == "finance") {
        return "node_finance_audit"
    }
    return "node_general_audit"
}
```

```groovy
// 操作者加载脚本：动态加载审批人
def run(request) {
    def dept = request.getFormData("department")
    def amount = request.getFormData("amount")
    // 大额需要财务总监
    if (amount > 50000) {
        return [1001L, 1002L]  // 返回用户 ID 列表
    }
    return [request.getCreatedOperatorId()]
}
```

```groovy
// 节点标题脚本：动态生成待办标题
def run(request) {
    def name = request.getCreatedOperatorName()
    def title = request.getWorkflowTitle()
    return "${name} 提交的 ${title} 待您审批"
}
```

```groovy
// 子流程脚本：创建子流程
def run(request) {
    return request.toCreateRequest(
        "sub-process-design-id",
        request.getCurrentOperatorId(),
        "submit",
        request.getFormData()
    )
}
```

```groovy
// 使用 $bind 查询额外数据
def run(request) {
    def record = $bind.getRecordById(request.getFormData("relatedRecordId"))
    def userService = $bind.getBean(com.example.UserService)
    def users = userService.findByDepartment(record.getDepartment())
    return users.collect { it.getUserId() }
}
```

## 注意事项

- **脚本入口签名**：所有 Groovy 脚本必须遵循 `def run(request){ ... }` 签名，`request` 即 `GroovyScriptRequest` 实例
- **双绑定对象**：脚本中同时可使用 `request`（本类）和 `$bind`（`GroovyScriptBind`），`request` 用于读取流程上下文，`$bind` 用于查询 Bean 和数据库
- **构造方式**：`GroovyScriptRequest` 由框架从 `FlowSession` 自动构建，不鼓励手动实例化
- **返回值类型**：不同脚本类型对返回值有不同要求 — 条件脚本返回 `Boolean`，路由脚本返回 `String`（节点 ID），操作者加载返回 `List<Long>`（用户 ID 列表），子流程脚本返回 `FlowCreateRequest`
- **表单数据类型**：`getFormData(String)` 返回 `Object`，实际类型取决于表单字段定义，使用时需自行类型转换
- **`submitOperator` 可为 null**：提交人信息可能未设置，调用 `getSubmitOperatorId()` 返回 0，`getSubmitOperatorName()` 返回 null
