---
name: flow-engine-framework/action
module: flow-engine-framework
description: 流程动作体系，包含 9 种动作类型、动作接口和动作工厂
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - IFlowAction
  - BaseAction
  - ActionDisplay
  - ActionType
  - FlowActionFactory
  - PassAction
  - RejectAction
  - TransferAction
  - ReturnAction
  - DelegateAction
  - AddAuditAction
  - ReduceAuditAction
  - CustomAction
  - SaveAction
  - InitiatorSelectNodeService
content_hash: 0ec57b5fb719f4df594be65eda1e92ba172dad9dfb2aa8f9ec272b275e0a8321
---

## 解决什么问题

提供流程节点上可执行动作的标准化体系，解决以下问题：

- **动作类型标准化**：9 种标准动作覆盖审批、驳回、转办、退回、委派、加签、减签、自定义、暂存场景
- **动作执行入口**：`IFlowAction.run()` 和 `generateRecords()` 分别对应动作触发和记录生成
- **动作可用性控制**：`enable()` 控制动作在当前节点是否可用
- **动作创建统一化**：`FlowActionFactory` 通过反射从 Map 数据统一反序列化动作实例

## 如何使用

### 动作类型

| 动作 | ActionType | 说明 |
|------|-----------|------|
| `PassAction` | PASS | 审批通过，触发下一节点 |
| `RejectAction` | REJECT | 驳回，退回至指定节点 |
| `TransferAction` | TRANSFER | 转办，将任务转给其他操作者 |
| `ReturnAction` | RETURN | 退回，退回至上一节点 |
| `DelegateAction` | DELEGATE | 委派，临时委派给其他操作者 |
| `AddAuditAction` | ADD_AUDIT | 加签，增加审批人（已在本节点待办中的不可重复加签） |
| `ReduceAuditAction` | REDUCE_AUDIT | 减签，撤下本节点其他待审批人（仅循序/并签，不可合并批量） |
| `CustomAction` | CUSTOM | 自定义动作，通过脚本定义行为 |
| `SaveAction` | SAVE | 暂存，保存当前状态 |

### 动作约束

| 动作 | 约束 | 异常 |
|------|------|------|
| 加签 | 目标不能在当前节点待办队列中（含顺序审批未激活的隐藏记录）；人数受 `maxOperatorCount` 限制 | `addAuditOperatorAlreadyInTodo`、`operatorCountExceeded`、`operatorNotInScope` |
| 减签 | 仅 `SEQUENCE`/`MERGE` 节点；当前节点至少 2 名待审批人；目标须为本节点其他待审批人 | `reduceAuditUnsupported`、`reduceAuditSingleApprover`、`reduceAuditOwnRecord`、`reduceAuditTargetNotFound`、`reduceAuditNoRemainingApproval` |

### 批量提交能力

| 项 | 说明 |
|------|------|
| 声明 | `IFlowAction.batchable()`，默认 `true` |
| `false` 的动作 | `ReduceAuditAction`（候选人/目标按流程实例隔离，合并批量会错配） |
| 下发 | detail 响应 `actions[].batchable`，前端据此决定合并列表是否展示 |

### 核心接口

`IFlowAction` 继承 `IMapConvertor`（Map 序列化）和 `ICopyAbility`（复制能力），主要方法：

| 方法 | 说明 |
|------|------|
| `type()` | 动作类型标识 |
| `id()` | 动作 ID |
| `title()` | 动作名称 |
| `display()` | 返回 `ActionDisplay` 展示配置 |
| `enable()` | 动作是否可用 |
| `show(FlowSession)` | 控制动作按钮在当前节点是否展示 |
| `batchable()` | 是否支持合并批量提交，默认 `true` |
| `run(FlowSession)` | 执行动作（触发流程流转） |
| `generateRecords(FlowSession)` | 生成流程记录 |

### BaseAction 扩展

`BaseAction` 提供 `triggerNode(FlowSession flowSession, Consumer<FlowSession> consumer)` 方法，消费下一个待办会话并递归触发后续节点，子类只需关注自身逻辑。

## 使用实例

```java
// 通过工厂从 Map 创建动作
Map<String, Object> actionData = Map.of(
    "type", "PASS",
    "id", "action-pass-1",
    "title", "通过",
    "enable", true
);
IFlowAction action = FlowActionFactory.getInstance().createAction(actionData);

// 执行动作
action.run(flowSession);

// 构建自定义动作（默认构造或从 Map 反序列化）
CustomAction customAction = CustomAction.defaultAction();
CustomAction fromMapAction = CustomAction.fromMap(actionData);
```
