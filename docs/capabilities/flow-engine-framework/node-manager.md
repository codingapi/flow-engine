---
name: flow-engine-framework/node-manager
module: flow-engine-framework
description: 流程节点管理器，负责节点查找、下一节点计算和节点状态管理
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - FlowNodeManager
  - FlowNodeState
  - ActionManager
  - NodeStrategyManager
  - OperatorManager
  - WorkflowStrategyManager
content_hash: 4337f32a692b35e73142c95e71e0722d160cd27c558ac9dc1bbad8017d007b60
---

## 解决什么问题

提供流程运行时节点的管理能力，解决以下问题：

- **节点查找**：`FlowNodeManager` 支持递归查找嵌套节点（包括分支节点内的子节点）
- **下一节点计算**：内部类 `FlowNodeSearcher` 实现复杂的下一节点路由逻辑，支持分支、并行、包容等结构
- **节点状态追踪**：`FlowNodeState` 封装节点运行时的状态判断（结束节点、分支节点、阻塞节点等）
- **策略与动作管理**：`NodeStrategyManager`、`ActionManager`、`OperatorManager`、`WorkflowStrategyManager` 分别管理节点策略、动作、操作者和工作流策略

## 如何使用

### FlowNodeManager

| 方法 | 说明 |
|------|------|
| `getFlowNode(String nodeId)` | 根据 ID 递归查找节点（含分支子节点） |
| `getNextNodes(IFlowNode current)` | 计算当前节点的下一节点列表 |

### 内部 FlowNodeSearcher 路由规则

| 当前节点类型 | 下一节点规则 |
|-------------|-------------|
| EndNode | 返回空列表（流程结束） |
| IBlockNode | 返回 blocks 子节点 |
| IBranchNode | 返回第一组分支子节点，为空则跳到外层下一节点 |
| 普通节点 | 返回列表中的下一个节点 |

### 管理器分工

| 管理器 | 职责 |
|--------|------|
| `FlowNodeManager` | 节点查找与下一节点计算 |
| `ActionManager` | 管理节点上的动作列表 |
| `NodeStrategyManager` | 管理节点上的策略列表 |
| `OperatorManager` | 管理节点的操作者列表 |
| `WorkflowStrategyManager` | 管理工作流级策略列表 |

## 使用实例

```java
// 创建节点管理器
List<IFlowNode> nodes = workflow.getNodes();
FlowNodeManager nodeManager = new FlowNodeManager(nodes);

// 查找节点
IFlowNode node = nodeManager.getFlowNode("approval-1");

// 获取下一节点
List<IFlowNode> nextNodes = nodeManager.getNextNodes(currentNode);

// FlowNodeState 状态判断
FlowNodeState state = new FlowNodeState(node);
boolean isEnd = state.isEndNode();
boolean isBranch = state.isBranchNode();
boolean isBlock = state.isBlockNode();
```
