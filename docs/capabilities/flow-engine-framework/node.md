---
name: flow-engine-framework/node
module: flow-engine-framework
description: 流程节点体系，包含 19 种节点类型、节点接口层次和节点工厂
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - IFlowNode
  - BaseFlowNode
  - BaseAuditNode
  - IBlockNode
  - IDisplayNode
  - NodeType
  - NodeFactory
  - ApprovalNode
  - ConditionNode
  - ConditionBranchNode
  - ConditionElseBranchNode
  - DelayNode
  - EndNode
  - HandleNode
  - InclusiveNode
  - InclusiveBranchNode
  - InclusiveElseBranchNode
  - ManualNode
  - ManualBranchNode
  - NotifyNode
  - ParallelNode
  - ParallelBranchNode
  - RouterNode
  - StartNode
  - SubProcessNode
  - TriggerNode
  - BackNodeHelper
  - ParallelNodeRelationHelper
content_hash: 9e34f8654dc5b4885ffd49afa8cc555ad4d5f1c62c5355ce7f313101e137029f
---

## 解决什么问题

提供工作流中流程节点的完整类型体系，解决以下问题：

- **节点类型标准化**：19 种节点类型覆盖审批、条件分支、并行、包容、延迟、子流程等常见流程场景
- **节点接口分层**：通过 `IFlowNode` → `BaseFlowNode` / `BaseAuditNode` → 具体节点的继承层次，区分逻辑节点和可显示节点
- **节点创建统一化**：`NodeFactory` 通过反射机制统一创建各类节点实例，支持从 Map 反序列化

## 如何使用

### 接口层次

```
IFlowNode（核心节点接口）
├── BaseFlowNode（基础流程节点）
│   ├── StartNode          — 开始节点
│   ├── EndNode            — 结束节点
│   ├── HandleNode         — 办理节点
│   ├── DelayNode          — 延迟节点
│   ├── RouterNode         — 路由节点
│   ├── SubProcessNode     — 子流程节点
│   ├── TriggerNode        — 触发器节点
│   ├── ConditionNode      — 条件节点 (IBlockNode)
│   ├── InclusiveNode      — 包容节点 (IBlockNode)
│   ├── ParallelNode       — 并行节点 (IBlockNode)
│   └── ManualNode         — 自选节点 (IBlockNode + IDisplayNode)
├── BaseAuditNode（审批基类）
│   ├── ApprovalNode       — 审批节点 (IDisplayNode)
│   └── NotifyNode         — 通知节点 (IDisplayNode)
└── 分支节点
    ├── ConditionBranchNode / ConditionElseBranchNode
    ├── InclusiveBranchNode / InclusiveElseBranchNode
    ├── ParallelBranchNode
    └── ManualBranchNode
```

### 标记接口

| 接口 | 用途 |
|------|------|
| `IDisplayNode` | 标记在审批流程中可见的节点（StartNode、EndNode、ApprovalNode 等） |
| `IBlockNode` | 标记包含子分支的容器节点（ConditionNode、ParallelNode、InclusiveNode 等） |

### NodeFactory 使用

通过 `NodeFactory.getInstance()` 获取单例，支持两种创建方式：
- `createNode(NodeType type)` — 按类型创建默认节点
- `createNode(Map<String, Object> map)` — 从 Map 数据反序列化节点

## 使用实例

```java
// 通过工厂创建默认节点
IFlowNode startNode = NodeFactory.getInstance().createNode(NodeType.StartNode);

// 从 Map 反序列化节点
Map<String, Object> nodeData = ...;
IFlowNode node = NodeFactory.getInstance().createNode(nodeData);

// 使用建造者创建节点
ApprovalNode node = ApprovalNode.builder()
    .id("approval-1")
    .name("部门审批")
    .actions(List.of(passAction, rejectAction))
    .strategies(List.of(routerStrategy, timeoutStrategy))
    .build();
```
