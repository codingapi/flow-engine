---
name: flow-engine-framework/action
module: flow-engine-framework
description: 流程动作体系，包含 8 种动作类型、动作接口和动作工厂
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
  - CustomAction
  - SaveAction
  - InitiatorSelectNodeService
content_hash: 0ec57b5fb719f4df594be65eda1e92ba172dad9dfb2aa8f9ec272b275e0a8321
---

## 解决什么问题

提供流程节点上可执行动作的标准化体系，解决以下问题：

- **动作类型标准化**：8 种标准动作覆盖审批、驳回、转办、退回、委派、加签、自定义、暂存场景
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
| `AddAuditAction` | ADD_AUDIT | 加签，增加审批人 |
| `CustomAction` | CUSTOM | 自定义动作，通过脚本定义行为 |
| `SaveAction` | SAVE | 暂存，保存当前状态 |

### 核心接口

`IFlowAction` 继承 `IMapConvertor`（Map 序列化）和 `ICopyAbility`（复制能力），主要方法：

| 方法 | 说明 |
|------|------|
| `run(FlowSession)` | 执行动作（触发流程流转） |
| `generateRecords(FlowSession)` | 生成流程记录 |
| `enable()` | 动作是否可用 |
| `display()` | 返回 `ActionDisplay` 展示配置 |

### BaseAction 扩展

`BaseAction` 提供 `triggerNode()` 方法递归触发后续节点，子类只需关注自身逻辑。

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

// 构建自定义动作
CustomAction customAction = CustomAction.builder()
    .id("custom-1")
    .title("特殊审批")
    .enable(true)
    .build();
```
