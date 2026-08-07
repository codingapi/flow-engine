---
name: flow-engine-framework/operator-assignment
module: flow-engine-framework
description: 操作人分配能力，支持发起人/审批人设定的分配校验与落库
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - OperatorAssignmentService
  - FlowOperatorAssignmentRepository
content_hash: f502894911c6aa57e630d8ae047de982d1b7833e635de334e5abafb33b73a48e
---

## 解决什么问题

提供操作人分配（发起人设定 / 审批人设定）的校验与持久化能力：

- **范围校验**：目标节点配置了可选人员范围时，校验所选人员是否全部落在范围内，越界抛出 `FlowValidationException.operatorOutOfRange`
- **分配落库**：校验通过后按流程实例与节点保存选定的操作人 ID 列表
- **幂等保存**：同一节点重复设定时覆盖旧分配

## 如何使用

### 核心组件

| 组件 | 职责 |
|------|------|
| `OperatorAssignmentService` | 静态服务，提供 `validateAndSave(baseSession, processId, operatorSelectMap)` 校验并保存操作人分配 |
| `FlowOperatorAssignmentRepository` | 操作人分配仓储接口，定义 `save(processId, nodeId, operatorIds)` 与 `findOperatorIds(processId, nodeId)` |
| `FlowValidationException.operatorOutOfRange` | 所选人员越界时抛出的异常 |

### 校验规则

- `operatorSelectMap` 为空时直接返回，不做处理
- 通过 `node.strategyManager().loadOperatorRange(session)` 加载节点可选人员范围；范围为空（未配置脚本或脚本执行结果为空）表示不限范围，跳过校验
- 范围非空时，所选人员必须全部落在范围内，否则抛出异常

## 使用实例

```java
// 在 FlowCreateService / FlowActionService 中调用
Map<String, List<Long>> operatorSelectMap = request.getOperatorSelectMap();
OperatorAssignmentService.validateAndSave(baseSession, processId, operatorSelectMap);
```