---
module: flow-engine-framework
name: ScriptRegistryContext
description: 脚本注册中心单例，管理各类默认 Groovy 脚本（路由、条件、触发、人员加载等），支持自定义替换默认脚本实现。
---

# ScriptRegistryContext

- **来源**: 自有
- **所属 module**: flow-engine-framework
- **Maven 坐标**: com.codingapi.flow:flow-engine-framework:0.0.28

## 何时使用

ScriptRegistryContext 是流程引擎中所有默认 Groovy 脚本的注册中心。当各脚本节点（条件、路由、触发、人员加载等）未配置自定义脚本时，通过此注册中心获取默认脚本。业务方如需全局替换某类脚本的默认行为，可通过 `setRegistry()` 注入自定义的 IScriptRegistry 实现。

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
| `ScriptRegistryContext` | `com.codingapi.flow.script.registry` | 脚本注册中心单例，委托 IScriptRegistry 提供默认脚本 |
| `IScriptRegistry` | `com.codingapi.flow.script.registry` | 脚本注册接口，定义 10 种脚本获取方法 |
| `DefaultScriptRegistry` | `com.codingapi.flow.script.registry` | 默认实现，返回 ScriptDefaultConstants 中的内置脚本 |
| `ScriptDefaultConstants` | `com.codingapi.flow.script` | 默认脚本常量定义类 |

### 关键方法

#### ScriptRegistryContext（单例，通过 `ScriptRegistryContext.getInstance()` 获取）

| 方法签名 | 返回值 | 说明 |
|----------|--------|------|
| `static getInstance()` | `ScriptRegistryContext` | 获取单例实例 |
| `setRegistry(IScriptRegistry)` | `void` | 替换脚本注册实现（不能为 null） |
| `getRouterScript()` | `String` | 获取路由脚本（决定流程走向） |
| `getNodeTitleScript()` | `String` | 获取节点标题脚本（生成待办标题） |
| `getConditionScript()` | `String` | 获取条件脚本（判断条件是否满足） |
| `getTriggerScript()` | `String` | 获取触发器脚本（执行触发动作） |
| `getSubProcessScript()` | `String` | 获取子流程脚本（创建子流程） |
| `getOperatorLoadScript()` | `String` | 获取操作者加载脚本（加载审批人） |
| `getOperatorMatchScript()` | `String` | 获取操作者匹配脚本（匹配操作人） |
| `getErrorTriggerScript()` | `String` | 获取错误触发脚本（处理异常） |
| `getActionCustomScript()` | `String` | 获取自定义动作脚本 |
| `getActionRejectScript()` | `String` | 获取拒绝动作脚本 |

#### 默认脚本内容（DefaultScriptRegistry 返回值）

| 脚本类型 | 默认行为 | 脚本 META |
|----------|----------|-----------|
| 路由脚本 | 返回开始节点 ID | `{"node":"START"}` |
| 节点标题脚本 | 返回 "你有一条待办" | 无 |
| 条件脚本 | 返回 true（允许执行） | 无 |
| 触发器脚本 | 打印 "hello trigger node." | 无 |
| 子流程脚本 | 返回 `request.toCreateRequest()` | 无 |
| 操作者加载脚本 | 返回流程创建者 ID | `{"type":"creator"}` |
| 操作者匹配脚本 | 返回 true（任意用户） | `{"type":"any"}` |
| 错误触发脚本 | 回退至开始节点 | `{"type":"node","node":"START"}` |
| 自定义动作脚本 | 触发通过 | `{"trigger":"PASS"}` |
| 拒绝动作脚本 | 返回开始节点 ID | `{"type":"START"}` |

### 调用方（使用默认脚本的脚本节点）

| 脚本节点 | 调用方法 |
|----------|----------|
| `ConditionScript` | `getInstance().getConditionScript()` |
| `OperatorLoadScript` | `getInstance().getOperatorLoadScript()` |
| `OperatorMatchScript` | `getInstance().getOperatorMatchScript()` |
| `RouterNodeScript` | `getInstance().getRouterScript()` |
| `TriggerScript` | `getInstance().getTriggerScript()` |
| `ErrorTriggerScript` | `getInstance().getErrorTriggerScript()` |
| `NodeTitleScript` | `getInstance().getNodeTitleScript()` |
| `SubProcessScript` | `getInstance().getSubProcessScript()` |
| `CustomScript` | `getInstance().getActionCustomScript()` |
| `RejectActionScript` | `getInstance().getActionRejectScript()` |

### 配置项

ScriptRegistryContext 自身无配置项。

## 使用示例

### 使用默认脚本（无需任何配置）

```java
// 各脚本节点的 defaultScript() 静态方法自动从注册中心获取默认脚本
ConditionScript script = ConditionScript.defaultScript();
```

### 自定义全部默认脚本

```java
public class CustomScriptRegistry implements IScriptRegistry {

    @Override
    public String getConditionScript() {
        return """
            // 自定义条件：金额大于 1000 才通过
            def run(request){
                def amount = request.getFormData('amount')
                return amount > 1000
            }
            """;
    }

    @Override
    public String getOperatorLoadScript() {
        return """
            // 自定义人员加载：返回部门经理
            def run(request){
                def myService = $bind.getBean(com.example.UserService.class)
                return myService.getManagerIds(request.getCurrentOperatorId())
            }
            """;
    }

    // ... 其他方法返回默认或自定义脚本
}

// 替换注册实现
ScriptRegistryContext.getInstance().setRegistry(new CustomScriptRegistry());
```

### 仅替换特定脚本

```java
// 继承 DefaultScriptRegistry，仅覆盖需要的方法
public class PartialCustomRegistry extends DefaultScriptRegistry {

    @Override
    public String getNodeTitleScript() {
        return """
            def run(request){
                return request.getCreatedOperatorName() + '的请假申请'
            }
            """;
    }

    // 其他方法使用默认实现
}

ScriptRegistryContext.getInstance().setRegistry(new PartialCustomRegistry());
```

## 注意事项

- **单例模式**：全局唯一实例，通过 `getInstance()` 获取，`setRegistry()` 会替换整个注册实现
- **默认实现**：未调用 `setRegistry()` 时使用 `DefaultScriptRegistry`，返回 `ScriptDefaultConstants` 中定义的内置脚本
- **setRegistry 不接受 null**：传入 null 会抛出 IllegalArgumentException
- **全局生效**：替换注册实现后，所有后续通过 `defaultScript()` / `any()` 等工厂方法创建的脚本节点都会使用新的默认脚本，已有的脚本实例不受影响
- **脚本优先级**：工作流设计器中为节点配置的自定义脚本优先于注册中心的默认脚本，注册中心仅在未配置自定义脚本时生效
- **线程安全**：setRegistry() 非线程安全，应在应用启动阶段（Spring 初始化前或初始化时）完成设置，避免运行时动态替换
