# ScriptRegistryContext

## 何时使用

当需要自定义工作流引擎中各类默认 Groovy 脚本（路由、条件判断、触发器、操作者匹配等）时，通过 `ScriptRegistryContext` 替换默认脚本实现。典型场景：

- 自定义路由逻辑（如按部门、角色分发）
- 替换操作者加载/匹配脚本以对接企业组织架构
- 自定义错误触发脚本以适配业务异常处理策略

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
| `ScriptRegistryContext` | `com.codingapi.flow.script.registry` | 脚本注册单例，提供获取各类默认脚本的能力 |
| `IScriptRegistry` | `com.codingapi.flow.script.registry` | 脚本注册接口，定义 10 种脚本获取方法 |
| `DefaultScriptRegistry` | `com.codingapi.flow.script.registry` | 默认脚本注册实现，返回 `ScriptDefaultConstants` 中的常量 |

### 关键方法

#### ScriptRegistryContext

| 方法签名 | 参数说明 | 返回值 | 说明 |
|----------|----------|--------|------|
| `getInstance()` | 无 | `ScriptRegistryContext` | 获取单例实例 |
| `setRegistry(IScriptRegistry)` | `registry` — 自定义脚本注册实现，不能为 null | `void` | 替换默认脚本注册实现 |
| `getRouterScript()` | 无 | `String` | 获取路由脚本 |
| `getNodeTitleScript()` | 无 | `String` | 获取节点标题脚本 |
| `getConditionScript()` | 无 | `String` | 获取条件脚本 |
| `getTriggerScript()` | 无 | `String` | 获取触发器脚本 |
| `getSubProcessScript()` | 无 | `String` | 获取子流程脚本 |
| `getOperatorLoadScript()` | 无 | `String` | 获取操作者加载脚本 |
| `getOperatorMatchScript()` | 无 | `String` | 获取操作者匹配脚本 |
| `getErrorTriggerScript()` | 无 | `String` | 获取错误触发脚本 |
| `getActionCustomScript()` | 无 | `String` | 获取自定义动作脚本 |
| `getActionRejectScript()` | 无 | `String` | 获取拒绝动作脚本 |

#### IScriptRegistry 接口方法

| 方法签名 | 说明 |
|----------|------|
| `getRouterScript()` | 路由脚本 — 用于网关节点的分支选择 |
| `getNodeTitleScript()` | 节点标题脚本 — 用于动态计算节点显示标题 |
| `getConditionScript()` | 条件脚本 — 用于条件分支节点的条件判断 |
| `getTriggerScript()` | 触发器脚本 — 用于流程触发逻辑 |
| `getSubProcessScript()` | 子流程脚本 — 用于子流程调用逻辑 |
| `getOperatorLoadScript()` | 操作者加载脚本 — 用于加载可操作人员列表 |
| `getOperatorMatchScript()` | 操作者匹配脚本 — 用于匹配当前操作者 |
| `getErrorTriggerScript()` | 错误触发脚本 — 用于异常场景的触发逻辑 |
| `getActionCustomScript()` | 自定义动作脚本 — 用于自定义动作执行逻辑 |
| `getActionRejectScript()` | 拒绝动作脚本 — 用于拒绝操作的执行逻辑 |

## 使用示例

```java
// 基础用法：获取默认脚本
ScriptRegistryContext registry = ScriptRegistryContext.getInstance();
String routerScript = registry.getRouterScript();
String conditionScript = registry.getConditionScript();
String operatorLoadScript = registry.getOperatorLoadScript();
```

```java
// 进阶用法：替换默认脚本注册实现
public class CustomScriptRegistry implements IScriptRegistry {

    @Override
    public String getRouterScript() {
        return "return 'branch-a'"; // 自定义路由逻辑
    }

    @Override
    public String getOperatorLoadScript() {
        return """
            def users = context.get('department').getUsers();
            return users.collect { it.name };
            """;  // 对接企业组织架构
    }

    // ... 其他方法使用默认实现或自行实现
}

// 在应用启动时注册
ScriptRegistryContext.getInstance().setRegistry(new CustomScriptRegistry());
```

## 注意事项

- **线程安全性**：`ScriptRegistryContext` 是饿汉式单例，`setRegistry()` 无同步保护，建议在应用启动阶段（Spring 容器初始化前）完成注册，避免运行时并发替换
- **替换不可逆**：`setRegistry()` 会完全替换原有注册实现，如需部分覆盖，应在自定义实现中委托给 `DefaultScriptRegistry`
- **null 安全**：`setRegistry(null)` 会抛出 `IllegalArgumentException`
- **版本兼容性**：当前版本为 `0.0.26`，接口方法可能随版本迭代增加，实现 `IScriptRegistry` 时需注意向前兼容
