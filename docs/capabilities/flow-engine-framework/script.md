---
name: flow-engine-framework/script
module: flow-engine-framework
description: Groovy 脚本运行时，包含脚本注册表、脚本类型封装和脚本执行上下文
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - IScriptRegistry
  - DefaultScriptRegistry
  - ScriptRegistryContext
  - FlowGroovyScriptFactory
  - ScriptDefaultConstants
  - ActionCustomScript
  - ActionRejectScript
  - ConditionScript
  - ErrorTriggerScript
  - NodeTitleScript
  - OperatorLoadScript
  - OperatorMatchScript
  - RouterNodeScript
  - SubProcessScript
  - TriggerScript
  - GroovyScriptBind
  - GroovyScriptRequest
  - GroovyWorkflowRequest
  - FlowScriptContext
  - IBeanFactory
content_hash: 42b4c00cdcb8aac4c1695f7d503b1ec953ab4ea3b3f92067e2312c31ea9d4a69
---

## 解决什么问题

提供流程引擎的 Groovy 脚本运行时能力，解决以下问题：

- **脚本注册表**：通过 `ScriptRegistryContext` 单例管理各类默认脚本，支持替换默认实现
- **脚本类型封装**：将各种脚本场景封装为强类型类（如 `RouterNodeScript`、`ConditionScript` 等）
- **脚本执行上下文**：`FlowScriptContext` 提供脚本 `$bind` 上下文（Spring Bean、流程记录、操作者查询）；脚本的实际执行由 `FlowScriptRuntimeContext` 结合 spring-boot 的 `GroovyScript` 完成
- **脚本请求封装**：`GroovyScriptRequest` / `GroovyWorkflowRequest` 封装脚本执行所需的参数

## 如何使用

### 脚本注册表

`ScriptRegistryContext.getInstance()` 提供获取各类默认脚本的能力：

| 方法 | 脚本类型 |
|------|---------|
| `getRouterScript()` | 路由脚本 — 决定下一节点走向 |
| `getConditionScript()` | 条件脚本 — 条件分支判断 |
| `getNodeTitleScript()` | 节点标题脚本 — 动态生成节点标题 |
| `getTriggerScript()` | 触发器脚本 — 自动触发条件 |
| `getSubProcessScript()` | 子流程脚本 — 子流程触发规则 |
| `getOperatorLoadScript()` | 操作者加载脚本 — 加载节点操作者 |
| `getOperatorMatchScript()` | 操作者匹配脚本 — 匹配操作者 |
| `getErrorTriggerScript()` | 错误触发脚本 — 异常处理规则 |
| `getActionCustomScript()` | 自定义动作脚本 — 自定义动作行为 |
| `getActionDisplayScript()` | 动作展示脚本 — 控制动作按钮是否展示 |
| `getActionRejectScript()` | 拒绝动作脚本 — 拒绝动作行为 |
| `getSubProcessResultScript()` | 子流程结果确认脚本 — 判断子流程结果（默认委托 `getConditionScript`） |

### 自定义脚本注册

```java
// 替换默认脚本注册实现
ScriptRegistryContext.getInstance().setRegistry(new IScriptRegistry() {
    @Override
    public String getRouterScript() {
        return "return customRouterLogic;";
    }
    // ... 其他方法
});
```

### 脚本执行上下文

`FlowScriptContext` 承载脚本的 `$bind` 上下文，实现 `IBeanFactory`，供 Groovy 脚本以 `$bind.getBean(...)`、`$bind.getRecordById(...)`、`$bind.getOperatorById(...)` 等方式访问 Spring 容器中的 Bean、流程记录与操作者信息；脚本的实际执行由 `FlowScriptRuntimeContext` 结合 spring-boot 的 `GroovyScript` 完成。

## 使用实例

```java
// 获取默认路由脚本
String routerScript = ScriptRegistryContext.getInstance().getRouterScript();

// 创建路由脚本实例并结合 FlowSession 执行
RouterNodeScript script = new RouterNodeScript(routerScript);
Object result = script.execute(flowSession);

// 替换默认脚本注册
ScriptRegistryContext.getInstance().setRegistry(customRegistry);
```
