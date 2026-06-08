---
name: flow-engine-framework/workflow-strategy
module: flow-engine-framework
description: 工作流级别策略体系，包含策略接口、工厂和具体策略实现
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - IWorkflowStrategy
  - BaseStrategy
  - WorkflowStrategyFactory
  - InterfereStrategy
  - UrgeStrategy
content_hash: c764fa48f064de57d0710ddb8deab0e6cec8505ab809b518b6606ae719c97972
---

## 解决什么问题

提供工作流级别的策略扩展能力（区别于节点级别的 `INodeStrategy`），解决以下问题：

- **流程级行为控制**：在工作流级别附加策略，影响整个流程的行为（而非单个节点）
- **干预策略**：`InterfereStrategy` 允许管理员在流程运行过程中介入干预
- **催办策略**：`UrgeStrategy` 定义流程催办的规则和间隔

## 如何使用

### 核心接口

`IWorkflowStrategy` 继承 `IMapConvertor`，通过 `TYPE_KEY` 字段标识策略类型。

### 策略类型

| 策略 | 说明 |
|------|------|
| `InterfereStrategy` | 干预策略，允许管理员在流程运行时干预流程状态 |
| `UrgeStrategy` | 催办策略，定义催办的时间间隔和规则 |

### 策略工厂

`WorkflowStrategyFactory.getInstance().createStrategy(map)` 根据 Map 中的 `strategyType` 字段反射创建策略实例。

### 与节点策略的区别

| 维度 | IWorkflowStrategy | INodeStrategy |
|------|-------------------|---------------|
| 作用范围 | 整个流程 | 单个节点 |
| 配置位置 | `Workflow.strategies` | `IFlowNode.strategies` |
| 典型场景 | 催办、干预 | 路由、超时、权限 |

## 使用实例

```java
// 通过工厂从 Map 创建工作流策略
Map<String, Object> strategyData = Map.of(
    "strategyType", "UrgeStrategy",
    "interval", 3600
);
IWorkflowStrategy strategy = WorkflowStrategyFactory.getInstance().createStrategy(strategyData);

// 在流程构建时附加工作流策略
Workflow workflow = WorkflowBuilder.builder()
    .id("flow-001")
    .strategies(List.of(
        new UrgeStrategy(3600),        // 每小时可催办一次
        new InterfereStrategy(true)    // 允许管理员干预
    ))
    .build();
```
