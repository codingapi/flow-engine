# GroovyScriptBind

## 何时使用

当需要在 Groovy 脚本中调用 Spring Bean、查询流程记录或查询操作人时，通过 `$bind` 变量使用。`GroovyScriptBind` 是脚本运行时注入的绑定对象，包装 `FlowScriptContext` 单例，使 Groovy 脚本能够访问应用层资源。典型场景：

- 在脚本中调用自定义 Spring Bean（如通知服务、审批规则服务）
- 在脚本中查询历史流程记录用于条件判断
- 在脚本中根据用户 ID 查询操作人详情

## 如何引用

### Maven 坐标

```xml
<dependency>
    <groupId>com.codingapi.flow</groupId>
    <artifactId>flow-engine-framework</artifactId>
    <version>0.0.26</version>
</dependency>
```

## API 说明

### 核心类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `GroovyScriptBind` | `com.codingapi.flow.script.request` | `$bind` 脚本绑定对象，委托给 `FlowScriptContext` |
| `FlowScriptContext` | `com.codingapi.flow.script.runtime` | 脚本上下文单例，持有 `IBeanFactory` 实现 |

### 关键方法

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getBean(Class<T>)` | `clazz` — Bean 类型 | `T` | 按类型获取 Spring Bean |
| `getBean(String, Class<T>)` | `name` — Bean 名称, `clazz` — Bean 类型 | `T` | 按名称和类型获取 Spring Bean |
| `getBeans(Class<T>)` | `clazz` — Bean 类型 | `List<T>` | 按类型获取所有 Spring Bean |
| `getRecordById(long)` | `id` — 记录 ID | `FlowRecord` | 根据 ID 获取流程记录 |
| `getOperatorById(long)` | `userId` — 操作人 ID | `IFlowOperator` | 根据 ID 获取操作人 |
| `findOperatorsByIds(List<Long>)` | `ids` — 操作人 ID 列表 | `List<IFlowOperator>` | 批量获取操作人 |

## 使用示例

```groovy
// 在 Groovy 脚本中使用 $bind
def run(request) {
    // 调用自定义 Spring Bean
    def notifyService = $bind.getBean(com.example.NotifyService)
    notifyService.send(request.getCurrentOperatorId(), "您有新的审批任务")

    return 'PASS'
}
```

```groovy
// 查询流程记录进行条件判断
def run(request) {
    def relatedId = request.getFormData("relatedRecordId")
    if (relatedId != null) {
        def record = $bind.getRecordById(Long.parseLong(relatedId.toString()))
        if (record != null && record.getStatus() == 'APPROVED') {
            return true
        }
    }
    return false
}
```

```groovy
// 查询操作人信息
def run(request) {
    def operatorIds = request.getFormData("approverIds") as List
    def operators = $bind.findOperatorsByIds(operatorIds.collect { it as Long })
    return operators.collect { it.getUserId() }
}
```

```groovy
// 结合 request 和 $bind 实现复杂逻辑
def run(request) {
    def dept = request.getFormData("department")
    def ruleService = $bind.getBean(com.example.ApprovalRuleService)
    def approverIds = ruleService.getApprovers(dept)
    return approverIds
}
```

## 注意事项

- **注入方式**：`$bind` 由 `ScriptRuntimeContext.execute()` 在每次脚本执行时自动注入，无需手动创建 `GroovyScriptBind` 实例
- **与 `request` 配合使用**：脚本中同时可用 `request`（`GroovyScriptRequest`，读取流程上下文）和 `$bind`（本类，访问应用资源），两者互补
- **委托机制**：所有方法调用最终委托给 `FlowScriptContext` 单例，实际行为取决于 `IBeanFactory` 的实现（由 Spring 自动配置注入）
- **Bean 查询依赖 Spring**：`getBean()` 系列方法仅在 Spring 环境中可用，单元测试需通过 `FlowScriptContext.setBeanFactory()` 注入 Mock
- **返回值可能为 null**：`getBean()` 在 Bean 不存在时返回 null，`getRecordById()` 在记录不存在时返回 null，脚本中需做空判断
