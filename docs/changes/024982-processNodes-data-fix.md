# processNodes 数据返回修复

> 编码: 024982 | 日期: 2026-04-28 | 调整类型: Bug修复 | 状态: 草稿

## 调整背景

FlowService 下的 processNodes 函数在返回的数据过程中存在多个问题，影响流程节点展示的准确性。

## 现状描述

### 问题1：历史记录未按 nodeId 合并（核心问题）

`FlowProcessNodeService.processNodes()` 中，为每条 FlowRecord 单独创建一个 ProcessNode：

```java
// FlowProcessNodeService.java:94-96, 107-109
for (FlowRecord historyRecord : historyRecords) {
    ProcessNode processNode = new ProcessNode(historyRecord, this.workflow);
    nodeList.add(processNode);
}
```

**影响**：当同一节点存在多条 FlowRecord（如多人审批场景），同一 nodeId 会生成多个 ProcessNode，每个仅包含一个审批人，导致前端重复展示同一节点。

### 问题2：`isDone()` 分支将 TODO 记录错误标记为历史节点

当 `flowRecord.isDone()` 为 true 时，加载该 processId 下的**所有**记录：

```java
// FlowProcessNodeService.java:93
List<FlowRecord> historyRecords = flowRecordService.findFlowRecordByProcessId(this.flowRecord.getProcessId());
```

**影响**：多人审批场景下，已审批完的用户的 `isDone()` 为 true，但同一 processId 下其他用户的记录可能仍在 TODO 状态。这些 TODO 记录会被错误标记为 `STATE_HISTORY`（因为构造函数固定设置 `state = STATE_HISTORY`）。

### 问题3：`equals()` 与 `hashCode()` 不一致

```java
// ProcessNode.java:108-119
public boolean equals(Object target) {
    // 只比较 nodeId
    return targetNode.getNodeId().equals(this.getNodeId());
}

public int hashCode() {
    // 包含 nodeId, nodeName, nodeType, state, operators
    return Objects.hash(nodeId, nodeName, nodeType, state, operators);
}
```

**影响**：违反 Java equals/hashCode 契约。两个 nodeId 相同但 state/operators 不同的 ProcessNode 会 `equals` 但 `hashCode` 不同，在 HashSet/HashMap 中使用时会导致数据丢失。同时影响 `NextNodeLoader` 中的去重逻辑正确性。

## 目标状态

1. 相同 nodeId 的历史记录合并为一个 ProcessNode，operators 列表包含所有审批人
2. TODO 状态的记录不被错误标记为历史节点
3. 修复 equals/hashCode 一致性，确保去重逻辑正确

## 调整范围

### 范围内

- `FlowProcessNodeService.processNodes()` — 历史记录合并逻辑
- `FlowProcessNodeService` 内部类 `NextNodeLoader` — 去重逻辑
- `ProcessNode` — equals/hashCode 修复、新增合并方法
- `ProcessNode(FlowRecord, Workflow)` 构造函数 — 调整 state 赋值逻辑

### 范围外

- FlowRecordService 接口（不修改查询方法签名）
- REST API 层（接口不变）
- 前端展示逻辑

## 约束与前提

- 不改变 `List<ProcessNode>` 返回类型
- 保持现有测试用例通过
- 修改后需执行编译验证

## 不调整的影响

- 多人审批节点在前端重复显示
- 已完成流程的节点状态可能不准确
- 在 HashSet 中使用 ProcessNode 可能丢失数据

## 相关资料

- `FlowProcessNodeService.java` (flow-engine-framework)
- `ProcessNode.java` (flow-engine-framework)
- `FlowRecordService.java` (flow-engine-framework)
- `FlowDetailServiceTest.java` (flow-engine-framework)

## 备注

涉及文件：
- `flow-engine-framework/src/main/java/com/codingapi/flow/service/impl/FlowProcessNodeService.java`
- `flow-engine-framework/src/main/java/com/codingapi/flow/pojo/response/ProcessNode.java`
