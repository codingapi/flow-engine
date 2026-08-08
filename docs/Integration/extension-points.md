# 节点 / 动作 / 策略扩展

Flow Engine 以「节点 × 动作 × 策略」三元组驱动流程行为。**节点/动作/策略类型由工厂硬编码注册，业务方不能新增类型**；行为差异通过 **Groovy 脚本** 与**策略配置**表达。

## 1. 类型总览

### 1.1 节点（19 种）

`NodeType` 枚举（`flow-engine-framework/.../node/NodeType.java`）：

```
APPROVAL, CONDITION, CONDITION_BRANCH, CONDITION_ELSE_BRANCH, DELAY, END,
HANDLE, INCLUSIVE, INCLUSIVE_BRANCH, INCLUSIVE_ELSE_BRANCH, MANUAL,
MANUAL_BRANCH, NOTIFY, PARALLEL, PARALLEL_BRANCH, ROUTER, START,
SUB_PROCESS, TRIGGER
```

| 节点 | 类型值 | 说明 |
|---|---|---|
| `StartNode` | `START` | 开始节点（发起流程生成首条记录） |
| `EndNode` | `END` | 结束节点（流程正常完成，推送 `FinishEvent`） |
| `ApprovalNode` | `APPROVAL` | 审批节点（多人审批策略） |
| `HandleNode` | `HANDLE` | 办理节点 |
| `NotifyNode` | `NOTIFY` | 抄送节点（生成抄送记录，不产生待办事件） |
| `ConditionNode` | `CONDITION` | 条件分支容器（块节点） |
| `ConditionBranchNode` | `CONDITION_BRANCH` | 条件分支（`ConditionScript` 判断） |
| `ConditionElseBranchNode` | `CONDITION_ELSE_BRANCH` | 其他情况 |
| `InclusiveNode` | `INCLUSIVE` | 包容分支容器（块节点） |
| `InclusiveBranchNode` | `INCLUSIVE_BRANCH` | 包容分支 |
| `InclusiveElseBranchNode` | `INCLUSIVE_ELSE_BRANCH` | 其他情况 |
| `ParallelNode` | `PARALLEL` | 并行分支容器（块节点，分支计数汇聚） |
| `ParallelBranchNode` | `PARALLEL_BRANCH` | 并行分支 |
| `ManualNode` | `MANUAL` | 人工节点（审批时需选择分支） |
| `ManualBranchNode` | `MANUAL_BRANCH` | 人工分支 |
| `RouterNode` | `ROUTER` | 路由节点（`RouterNodeScript` 返回目标） |
| `DelayNode` | `DELAY` | 延迟节点（定时触发） |
| `TriggerNode` | `TRIGGER` | 触发节点（`TriggerScript` 副作用） |
| `SubProcessNode` | `SUB_PROCESS` | 子流程节点 |

### 1.2 动作（8 种）

`ActionType` 枚举（`flow-engine-framework/.../action/ActionType.java`）：

```
SAVE, PASS, REJECT, ADD_AUDIT, DELEGATE, RETURN, TRANSFER, CUSTOM
保存   通过   拒绝   加签      委派     退回    转办    自定义
```

| 动作 | 特有配置 | 说明 |
|---|---|---|
| `SaveAction` | — | 仅保存表单，不流转 |
| `PassAction` | — | 通过 → 推进入下一节点；多人审批按策略自动办结/激活 |
| `RejectAction` | `ActionRejectScript` | 拒绝 → 退回指定节点/退回上级/终止流程（`TERMINATE`） |
| `AddAuditAction` | `OperatorLoadScript` + `maxOperatorCount` | 加签（追加审批人） |
| `DelegateAction` | `OperatorLoadScript` + `maxOperatorCount` | 委派（他人代审） |
| `ReturnAction` | — | 退回（目标由 `FlowAdvice.backNode` 提供） |
| `TransferAction` | `OperatorLoadScript` + `maxOperatorCount` | 转办 |
| `CustomAction` | `ActionCustomScript` + `triggerFrontEvent` | 自定义动作（脚本返回动作类型） |

### 1.3 节点策略（15 种）

`NodeStrategyFactory` 注册（key = 类简单名）：

| 策略 | 作用 | 关键枚举/字段 |
|---|---|---|
| `OperatorLoadStrategy` | 操作人配置 | `OperatorSelectType { SCRIPT, INITIATOR_SELECT, APPROVER_SELECT }`、`maxOperatorCount` |
| `MultiOperatorAuditStrategy` | 多人审批 | `Type { SEQUENCE, MERGE, ANY, RANDOM_ONE }`、`percent` |
| `SameOperatorAuditStrategy` | 同人重复审批 | `Type { AUTO_PASS, MANUAL_PASS }` |
| `AdviceStrategy` | 审批意见 | `adviceRequired` / `signRequired` / `adviceHidden` |
| `NodeTitleStrategy` | 节点标题 | `NodeTitleScript` |
| `FormFieldPermissionStrategy` | 表单字段权限 | `List<FormFieldPermission>`（`PermissionType { READ, WRITE, HIDDEN }`） |
| `ErrorTriggerStrategy` | 无匹配操作人兜底 | `ErrorTriggerScript` → `ErrorThrow`（跳节点/兜底操作人） |
| `TimeoutStrategy` | 超时 | `Type { REMIND, PASS, REJECT }`、`timeoutTime` |
| `DelayStrategy` | 延迟 | `Type { SECOND, MINUTE, HOUR, DAY }`、`time` |
| `TriggerStrategy` | 触发 | `TriggerScript` |
| `RouterStrategy` | 路由 | `RouterNodeScript` |
| `SubProcessStrategy` | 子流程 | `submit`、`SubProcessScript`、`SubProcessResultScript`、`showParentProcessRecords` |
| `ResubmitStrategy` | 退回后重提交 | `Type { RESUME, CHAIN }` |
| `RevokeStrategy` | 撤回 | `enable`、`Type { REVOKE_NEXT, REVOKE_CURRENT }` |
| `RecordMergeStrategy` | 记录合并 | `enable` |

### 1.4 工作流策略（2 种）

`WorkflowStrategyFactory` 注册（`strategy/workflow/` 包）：

| 策略 | 作用 | 字段 |
|---|---|---|
| `InterfereStrategy` | 干预（流程管理员可强制审批任意待办） | `enable`（默认 true） |
| `UrgeStrategy` | 催办间隔控制 | `enable`、`interval`（秒，默认 60） |

## 2. 扩展方式（业务方视角）

### 2.1 工厂注册：硬编码，不能扩展类型

`NodeFactory`、`FlowActionFactory`、`NodeStrategyFactory`、`WorkflowStrategyFactory` 均为**饿汉单例 + 私有构造内硬编码注册表 + 反射调用约定的静态方法**，无公开 `register` 方法。因此业务方**不能**新增节点/动作/策略类型。

### 2.2 行为表达：Groovy 脚本 + 策略配置

类型内行为差异全部通过脚本与配置表达：

| 想实现的能力 | 用什么 |
|---|---|
| 操作人 = 指定角色/部门/上级 | 自定义 `OperatorLoadScript` |
| 操作人 = 发起人指定 / 审批人指定 | `OperatorLoadStrategy.OperatorSelectType.INITIATOR_SELECT / APPROVER_SELECT` + `FlowOperatorAssignmentRepository` |
| 限制可选人数 | `OperatorLoadStrategy.maxOperatorCount`（-1 不限制） |
| 条件流转 | `ConditionScript`（条件/包容分支） |
| 动态路由 | `RouterNodeScript` |
| 表单字段只读/隐藏 | `FormFieldPermissionStrategy` |
| 节点标题 | `NodeTitleScript` |
| 自定义动作 | `CustomAction` + `ActionCustomScript` |
| 拒绝策略 | `ActionRejectScript`（退回节点 / 终止 `TERMINATE`） |
| 无匹配操作人兜底 | `ErrorTriggerStrategy` + `ErrorTriggerScript` |
| 定时提醒/自动通过/自动拒绝 | `TimeoutStrategy` |
| 延迟执行 | `DelayStrategy` |
| 子流程 | `SubProcessStrategy` + `SubProcessScript` |

### 2.3 脚本访问 Spring Bean

业务方服务可注入 `FlowScriptContext`（单例）或直接在脚本内通过 `$bind.getBean(...)` 调用（见 [流程默认脚本](./script-integration.md#22-groovyscriptbindbind-绑定对象)）。

## 3. 内部执行生命周期

节点执行遵循统一生命周期（`IFlowNode` 接口 javadoc）：

```
IFlowAction#run
  → 节点 isFinish 判断（多人审批策略）
  → BaseAction#triggerNode 递归分析下一节点
    → matchNextNodes（经 filterBranches 分支过滤）
    → node.handle(session)      true=继续递归；false=停止并生成记录
      → generateCurrentRecords  构建当前节点流程记录
      → fillNewRecord           保存时填充
```

- `BaseFlowNode.handle()` 默认实现含并行汇聚判定（`isWaitRecordMargeParallelNode`）。
- `BaseAuditNode`（审批/办理/抄送）：`handle()` 恒返回 `false`（人工节点停在此处）；`generateCurrentRecords` 带 `visitedNodeIds + depth` 防 `ErrorTrigger` 死循环/超深。
- 跳转限制：`BaseFlowNode.verifyJumpTarget` 拒绝跳转到 `Notify`/`SubProcess` 节点。

## 4. 经理层 API（`manager/` 包）

业务方可在脚本/事件处理中通过 `FlowSession` 访问节点配置：

| 类 | 用途 |
|---|---|
| `NodeStrategyManager` | 节点全部策略的门面：`loadOperators(session)`、`getMaxOperatorCount()`、`getOperatorSelectType()`、`generateTitle(session)`、`errorTrigger(session)`、`getFieldPermissions()`、`isAdviceRequired()` 等 |
| `ActionManager` | 动作管理：`filterActions(session)`、`getActionById(id)`、`getActionByType(type)`、`verifySession(session)` |
| `OperatorManager` | 操作人集合：`isEmpty()`、`match(operator)`、`getOperator(userId)` |
| `WorkflowStrategyManager` | 流程策略：`isEnableInterfere()`、`verifyOperator` |
| `FlowNodeManager` | 节点图：`getFlowNode(nodeId)`、`getNextNodes(node)` |

## 5. 异常体系

基类 `FlowException extends LocaleMessageException`，5 个子类（`exception/` 包），均以静态工厂方法构造：

| 子类 | 典型工厂方法与错误场景 |
|---|---|
| `FlowValidationException` | `fieldReadOnly`（字段只读）、`operatorCountExceeded(nodeId, max)`（超出最大可选人数）、`operatorOutOfRange`（超出可选范围）、`required` 等 |
| `FlowExecutionException` | `scriptExecutionError`、`routerNodeNotFound`、`operatorCountExceeded`、`invalidJumpTarget(nodeType)`、`errorTriggerLoop`、`subProcessLoop` 等 |
| `FlowStateException` | `repositoryNotRegistered`、`recordAlreadyDone`、`operatorNotMatch`、`workflowAlreadyDisable`、`recordNotSupportRevoke` 等 |
| `FlowNotFoundException` | `workflow(workCode)`、`record(recordId)`、`node(nodeId)`、`operator(operatorId)`、`action(actionId)` |
| `FlowPermissionException` | `accessDenied(operation)` |

REST 层经 `LocaleMessageException` 统一转换为 `{ errCode, errMessage }` 响应。