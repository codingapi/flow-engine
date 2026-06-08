---
name: flow-engine-framework/node-strategy
module: flow-engine-framework
description: 节点策略体系，包含 15 种策略类型、策略接口、策略工厂和策略管理器
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - INodeStrategy
  - BaseStrategy
  - NodeStrategyFactory
  - OperatorSelectType
  - AdviceStrategy
  - DelayStrategy
  - ErrorTriggerStrategy
  - FormFieldPermissionStrategy
  - MultiOperatorAuditStrategy
  - NodeTitleStrategy
  - OperatorLoadStrategy
  - RecordMergeStrategy
  - ResubmitStrategy
  - RevokeStrategy
  - RouterStrategy
  - SameOperatorAuditStrategy
  - SubProcessStrategy
  - TimeoutStrategy
  - TriggerStrategy
content_hash: b476b5ae6fa7f1d9c55e315240f11135b748ee3ef751b88f92084996dd7bfe28
---

## 解决什么问题

提供流程节点的可配置策略扩展体系，解决以下问题：

- **策略可插拔**：每个节点可附加多种策略，通过策略组合实现复杂业务规则
- **节点验证**：`verifyNode()` 在流程配置完成后验证节点配置的合法性
- **会话验证**：`verifySession()` 在流程执行前验证请求参数是否满足节点要求
- **策略创建统一化**：`NodeStrategyFactory` 通过反射从 Map 数据统一反序列化策略实例

## 如何使用

### 策略类型

| 策略 | 说明 |
|------|------|
| `RouterStrategy` | 路由策略，定义节点间的流转规则 |
| `TimeoutStrategy` | 超时策略，定义节点超时处理规则 |
| `DelayStrategy` | 延迟策略，定义节点延迟执行规则 |
| `TriggerStrategy` | 触发器策略，定义自动触发条件 |
| `ErrorTriggerStrategy` | 错误触发策略，定义异常时的处理规则 |
| `RevokeStrategy` | 撤销策略，定义流程撤销规则 |
| `ResubmitStrategy` | 重新提交策略，定义驳回后重新提交规则 |
| `SubProcessStrategy` | 子流程策略，定义子流程触发规则 |
| `NodeTitleStrategy` | 节点标题策略，动态生成节点标题 |
| `OperatorLoadStrategy` | 操作者加载策略，定义节点操作者的加载方式 |
| `FormFieldPermissionStrategy` | 表单字段权限策略，控制表单字段的读写权限 |
| `MultiOperatorAuditStrategy` | 多人审批策略，定义多人审批时的通过规则 |
| `SameOperatorAuditStrategy` | 相同操作者审批策略，处理审批人与上一节点相同的情况 |
| `RecordMergeStrategy` | 记录合并策略，定义待办记录的合并规则 |
| `AdviceStrategy` | 意见策略，定义审批意见的规则 |

### 核心接口

`INodeStrategy` 继承 `IMapConvertor` 和 `ICopyAbility<INodeStrategy>`：

| 方法 | 说明 |
|------|------|
| `verifyNode(FlowForm)` | 流程配置验证 |
| `verifySession(FlowSession)` | 执行前会话验证 |
| `strategyType()` | 返回策略类型标识（默认类名） |

### 策略工厂

`NodeStrategyFactory.getInstance().createStrategy(map)` 根据 Map 中的 `strategyType` 字段反射创建策略实例。

## 使用实例

```java
// 通过工厂从 Map 创建策略
Map<String, Object> strategyData = Map.of(
    "strategyType", "RouterStrategy",
    "script", "return nextNodeId;"
);
INodeStrategy strategy = NodeStrategyFactory.getInstance().createStrategy(strategyData);

// 配置节点策略
ApprovalNode node = ApprovalNode.builder()
    .id("approval-1")
    .strategies(List.of(
        new RouterStrategy(routerScript),
        new TimeoutStrategy(3600, "auto-pass"),
        new FormFieldPermissionStrategy(fieldPermissions)
    ))
    .build();

// 验证节点配置
strategy.verifyNode(flowForm);

// 验证会话
strategy.verifySession(flowSession);
```
