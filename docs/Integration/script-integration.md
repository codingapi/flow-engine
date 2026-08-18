# 流程默认脚本（Groovy 脚本机制）

流程引擎的节点行为、动作行为、操作人加载等大量能力通过 **Groovy 脚本**表达。脚本内容以 18 位动态生成的 key 注册，并固化进流程运行时快照。本文档介绍脚本的类型、执行上下文 API、默认脚本、以及如何替换默认脚本。

## 1. 脚本类型总览（12 种）

脚本由 `FlowGroovyScriptFactory`（`flow-engine-framework/.../script/factory/FlowGroovyScriptFactory.java`）创建。每种脚本有固定的 `typeOne`（恒为 `"flow"`）、`typeTwo`（脚本类型标识）、返回类型与请求对象：

| 脚本类型（typeTwo） | 工厂方法 | returnType | 请求对象 | 所在包装类 |
|---|---|---|---|---|
| `router-script` | `createRouterScript` | `String` | `GroovyScriptRequest` | `RouterNodeScript` |
| `node-title` | `createNodeTitleScript` | `String` | `GroovyScriptRequest` | `NodeTitleScript` |
| `condition` | `createConditionScript` | `Boolean` | `GroovyScriptRequest` | `ConditionScript` |
| `trigger` | `createTriggerScript` | `Void` | `GroovyScriptRequest` | `TriggerScript` |
| `sub-process` | `createSubProcessScript` | `Object` | `GroovyScriptRequest` | `SubProcessScript` |
| `sub-process-result` | `createSubProcessResultScript` | `Boolean` | `GroovyScriptRequest` | `SubProcessResultScript` |
| `operator-load` | `createOperatorLoadScript` | `List` | `GroovyScriptRequest` | `OperatorLoadScript` |
| `operator-match` | `createOperatorMatchScript` | `Boolean` | **`GroovyWorkflowRequest`** | `OperatorMatchScript` |
| `error-trigger` | `createErrorTriggerScript` | `Object` | `GroovyScriptRequest` | `ErrorTriggerScript` |
| `action-display` | `createActionDisplayScript` | `Boolean` | `GroovyScriptRequest` | `ActionDisplayScript` |
| `action-custom` | `createActionCustomScript` | `String` | `GroovyScriptRequest` | `ActionCustomScript` |
| `action-reject` | `createActionRejectScript` | `String` | `GroovyScriptRequest` | `ActionRejectScript` |

所有脚本的 `method` 均为 `"run"`，`binds` 均为 `{ "$bind": GroovyScriptBind.class }`。

### 脚本在引擎中的使用位置

| 脚本 | 用途 | 使用位置 |
|---|---|---|
| `RouterNodeScript` | 路由节点选择下一节点（返回目标 nodeId） | `RouterStrategy` |
| `NodeTitleScript` | 生成节点标题（待办标题） | `NodeTitleStrategy`（`NodeStrategyManager.generateTitle`） |
| `ConditionScript` | 条件分支判断（返回 true/false） | `ConditionBranchNode`、`InclusiveBranchNode` |
| `TriggerScript` | 触发节点执行副作用（忽略返回值） | `TriggerStrategy` |
| `SubProcessScript` | 生成子流程发起请求（返回 `FlowCreateRequest` 或集合） | `SubProcessStrategy` |
| `SubProcessResultScript` | 判定子流程是否全部结束 | `SubProcessStrategy` |
| `OperatorLoadScript` | 加载节点操作人（返回用户 id 列表） | `OperatorLoadStrategy` |
| `OperatorMatchScript` | 校验发起人是否允许发起流程 | `Workflow.matchCreatedOperator` |
| `ErrorTriggerScript` | 匹配不到操作人时的兜底（跳节点 / 兜底操作人） | `ErrorTriggerStrategy` |
| `ActionDisplayScript` | 控制动作是否展示 | `ActionDisplay.show` |
| `ActionCustomScript` | 自定义动作类型（返回 `SAVE/PASS/REJECT/ADD_AUDIT/DELEGATE/RETURN/TRANSFER`） | `CustomAction` |
| `ActionRejectScript` | 拒绝动作的目标（返回节点 id 或 `TERMINATE`） | `RejectAction` |

## 2. 脚本执行上下文

### 2.1 `GroovyScriptRequest`（运行期请求对象）

文件：`flow-engine-framework/.../script/request/GroovyScriptRequest.java`

构造：`new GroovyScriptRequest(FlowSession session)`，从会话提取完整上下文。`@ScriptFunction` 暴露的方法（脚本内可直接调用）：

**流程信息**

```groovy
request.getWorkflowId()          // 流程 id
request.getWorkflowTitle()       // 流程标题
request.getWorkflowCode()        // 流程编码
request.getNodeId() / getNodeName() / getNodeType()   // 当前节点
```

**表单数据**

```groovy
request.getFormData()                       // Map<String,Object> 全部表单值
request.getFormData("fieldCode")            // 单字段值
request.resetFormData(Map)                  // 覆盖表单数据
request.getSubFormData("subFormCode")       // List<Map<String,Object>> 子表单数据
```

**操作人信息**

```groovy
request.getCreatedOperator() / getCreatedOperatorId() / getCreatedOperatorName()   // 流程创建者
request.getSubmitOperator() / getSubmitOperatorId() / getSubmitOperatorName()     // 流程提交人
request.getCurrentOperator() / getCurrentOperatorId() / getCurrentOperatorName()  // 当前审批人
request.findPreviousNodeOperatorIds()       // List<Long> 上一节点实际审批人 id（排除自动办结）
```

**节点与流程控制**

```groovy
request.getNode("nodeId")                   // 按 id 取节点
request.getCurrentNode()                    // 当前节点
request.getStartNode()                      // 开始节点
request.isFlowManager()                     // 当前操作人是否流程管理员
request.isMock()                            // 是否 Mock 模式
request.getCurrentAction()                  // 当前动作
```

**子流程**

```groovy
request.findSubProcessRecords("subProcessNodeId")   // List<FlowRecord> 子流程记录
request.getSubProcessTotal()                        // 子流程总数
request.getCurrentSubProcessRecord()                // 当前子流程记录
```

**发起请求构建（供 SubProcessScript 使用）**

```groovy
request.toCreateRequest()                          // 用当前流程 code + 当前操作人 + 开始节点 SAVE 动作
request.toCreateRequest("workCode", operatorId, "actionId", "formDataJson")
request.toCreateRequest("workCode", operatorId, "actionId", formDataMap)
```

### 2.2 `GroovyScriptBind`（$bind 绑定对象）

文件：`flow-engine-framework/.../script/request/GroovyScriptBind.java`

脚本内通过 `$bind` 访问引擎外部资源：

```groovy
$bind.getBean(MyService.class)                      // 获取 Spring Bean 实例
$bind.getBean("beanName", MyService.class)
$bind.getBeans(MyService.class)                     // 获取全部实现
$bind.getRecordById(recordId)                       // 按 id 获取流程记录 FlowRecord
$bind.getOperatorById(userId)                       // 获取操作人
$bind.findOperatorsByIds([1L, 2L, 3L])              // 批量获取操作人
```

`$bind` 的后端是 `FlowScriptContext` 单例，其 `IBeanFactory` 由 starter 的 `FlowScriptContextRegister` 注入（转发到 Spring 容器）。

### 2.3 `GroovyWorkflowRequest`（发起匹配请求）

文件：`flow-engine-framework/.../script/request/GroovyWorkflowRequest.java`

仅用于 `OperatorMatchScript`（流程创建者校验）：

```groovy
request.getCurrentOperator()    // 发起人 IFlowOperator
request.getWorkflow()           // Workflow 对象（可访问 form/nodes 等）
```

## 3. 默认脚本（`ScriptDefaultConstants`）

文件：`flow-engine-framework/.../script/ScriptDefaultConstants.java`，定义 12 个默认脚本内容常量。每个脚本约定首行 `// @SCRIPT_TITLE <标题>`，可选第二行 `// @SCRIPT_META <JSON>`（供前端设计器解析）。

| 常量 | 标题 | META | 默认脚本 |
|---|---|---|---|
| `SCRIPT_DEFAULT_ACTION_CUSTOM` | 默认条件 触发通过 | `{"trigger":"PASS"}` | `def run(request){ return 'PASS'; }` |
| `SCRIPT_DEFAULT_ACTION_DISPLAY` | 默认显示 | — | `def run(request){ return true; }` |
| `SCRIPT_DEFAULT_ACTION_REJECT` | 返回开始节点 | `{"type":"START"}` | `def run(request){ return request.getStartNode().getId(); }` |
| `SCRIPT_DEFAULT_CONDITION` | 默认条件（允许执行） | — | `def run(request){ return true; }` |
| `SCRIPT_DEFAULT_ERROR_TRIGGER` | 回退至开始节点 | `{"type":"node","node":"START"}` | `def run(request){ return request.getStartNode().getId(); }` |
| `SCRIPT_DEFAULT_NODE_TITLE` | 你有一条待办 | — | `def run(request){ return '你有一条待办' }` |
| `SCRIPT_DEFAULT_OPERATOR_LOAD` | 流程创建者 | `{"type":"creator"}` | `def run(request){ return [request.getCreatedOperatorId()] }` |
| `SCRIPT_DEFAULT_OPERATOR_MATCH` | 任意用户 | `{"type":"any"}` | `def run(request){ return true }` |
| `SCRIPT_DEFAULT_ROUTER` | 发起节点 | `{"node":"START"}` | `def run(request){ return request.getStartNode().getId(); }` |
| `SCRIPT_DEFAULT_SUB_PROCESS` | 创建当前流程 | — | `def run(request){ return request.toCreateRequest() }` |
| `SCRIPT_DEFAULT_SUB_PROCESS_RESULT` | 全部子流程结束后继续 | — | `def run(request){ return request.findSubProcessRecords(request.getCurrentNode().getId()).size() == request.getSubProcessTotal(); }` |
| `SCRIPT_DEFAULT_TRIGGER` | 示例触发节点（打印触发日志） | — | `def run(request){ print('hello trigger node.\n'); }` |

## 4. 脚本注册与生命周期

### 4.1 脚本对象（`GroovyScript`）

来自外部依赖 `com.codingapi.springboot:springboot-starter-script:17.3.0`。构造方式：

```java
GroovyScript groovyScript = GroovyScript.builder(key)
        .script(scriptContent)
        .method("run")
        .returnType(String.class)
        .typeOne("flow")
        .typeTwo("router-script")
        .binds(Map.of("$bind", GroovyScriptBind.class))
        .requests(Map.of("request", GroovyScriptRequest.class))
        .build();
```

### 4.2 生命周期（临时 → 正式 → 版本隔离）

```
工厂创建（key = FlowIDGeneratorGatewayContext.generateFlowScriptKey()，18 位）
  → groovyScript.temp()          临时注册（TempGroovyScriptContext，15 分钟有效）
  → 保存流程时扫描 @GroovyScript 字段批量 save() 落库（正式注册）
  → 版本创建时 resetScripts() 将每个 key copy(新key) 实现版本隔离
```

- **扫描**：`GroovyScriptAnnotationScannerUtils.findGroovyScriptFields(workflow)` 扫描对象图中所有 `@GroovyScript` 标记的 String 字段（存的是 key）。
- **保存**：`WorkflowService.saveWorkflowVersion` 内调用 `WorkflowGroovyScriptUtils.saveScripts(workflow)`。
- **版本隔离**：`WorkflowGroovyScriptUtils.resetScripts` 把每个 key 的脚本内容 `copy()` 到新 key 并保存、回写字段。
- **删除**：`WorkflowGroovyScriptUtils.deleteScripts` → `GroovyScriptRepositoryContext.delete(key)`。

### 4.3 运行时快照隔离（关键机制）

- 流程发起时 `WorkflowRuntime` 构造器执行 `snapshotScripts()`，把流程全部脚本内容固化为 `Map<key, 内容>` 存入 `WorkflowRuntime.scripts`。
- 引擎各入口（`FlowCreateService`/`FlowActionService`/……）把快照写入 `FlowRuntimeScriptLocalCache`（ThreadLocal）。
- `FlowScriptRuntimeContext.getGroovyScript(key)` 在运行态用**快照内容**重建独立 `GroovyScript`，而编译缓存以脚本内容 SHA256 为键 —— 因此**设计期修改脚本不会影响在途流程**。

### 4.4 脚本仓储（下游必须提供）

脚本持久化依赖外部 SPI，业务方必须提供两个实现并自注册（参考 `flow-engine-example`）：

```java
// 1. 正式脚本仓储
public class GroovyScriptRepositoryImpl implements GroovyScriptRepository,
        InitializingBean {
    @Override
    public void afterPropertiesSet() {
        GroovyScriptRepositoryContext.getInstance().setGroovyScriptRepository(this);
    }
    // save/delete/get 委托自己的持久化
}

// 2. 临时脚本仓储
public class TempGroovyScriptRepositoryImpl implements TempGroovyScriptRepository,
        InitializingBean {
    @Override
    public void afterPropertiesSet() {
        TempGroovyScriptRepositoryContext.getInstance().setTempGroovyScriptRepository(this);
    }
    // get/save/delete/find(PageRequest)
}
```

## 5. 替换默认脚本

### 5.1 全局替换（`IScriptRegistry`）

实现 `IScriptRegistry` 接口并注册到单例，即可替换**全部默认脚本**（未配置脚本时使用你提供的默认值）：

```java
public class CustomScriptRegistry implements IScriptRegistry {
    @Override public String getRouterScript() { /* 返回 key 或 null */ }
    @Override public String getNodeTitleScript() { return null; }
    // ... 12 个方法全部实现（返回 null 表示无默认脚本）
}

// 启动时注册
ScriptRegistryContext.getInstance().setRegistry(new CustomScriptRegistry());
```

接口定义（`flow-engine-framework/.../script/registry/IScriptRegistry.java`）：

```java
public interface IScriptRegistry {
    String getRouterScript();
    String getNodeTitleScript();
    String getConditionScript();
    String getTriggerScript();
    String getSubProcessScript();
    default String getSubProcessResultScript() { return getConditionScript(); }
    String getOperatorLoadScript();
    String getOperatorMatchScript();
    String getErrorTriggerScript();
    String getActionCustomScript();
    String getActionDisplayScript();
    String getActionRejectScript();
}
```

默认实现 `DefaultScriptRegistry.getXxxScript()` 调用 `FlowGroovyScriptFactory.createXxxScript(ScriptDefaultConstants.SCRIPT_DEFAULT_XXX)` 创建临时脚本并返回其 key。

### 5.2 单流程配置（`@GroovyScript` 字段）

每个节点的策略/动作对象中的脚本字段（如 `OperatorLoadStrategy.operatorLoadScript`）本身就是 `@GroovyScript` 标记的字符串字段，通过设计器或编程式构建写入具体的脚本 key 即可实现单流程定制。

## 6. 脚本执行引擎（外部 starter-script）

- 编译/执行：`GroovyScriptRuntime`（`GroovyShell` + 按 SHA256 内容键控的 LRU 编译缓存，默认上限 10240）。
- 临时脚本缓存：`TempGroovyScriptContext`，默认 15 分钟过期（`codingapi.script.tempValidTime`）。
- 脚本对象缓存：`GroovyScriptCacheContext`（按 key，LRU 10*1024）。
- REST 接口：`/api/groovy-script/*`（compile / getScript / getMetadata / save），由依赖 starter 自动注册。

## 7. 脚本辅助工具

`GroovyScriptUtils`（`flow-engine-framework/.../script/utils/GroovyScriptUtils.java`）：

```java
clearComments(String script)     // 去除 // 单行注释后 trim
getReturnScript(String script)   // 提取 def run(...) 中的 return 表达式（供 ActionCustomScript.getTriggerType 静态解析触发类型）
```

`ActionCustomScript.getTriggerType()` 不执行脚本，直接静态解析脚本里的 return 字面量得到动作触发类型。