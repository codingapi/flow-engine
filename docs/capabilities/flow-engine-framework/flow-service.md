---
name: flow-engine-framework/flow-service
module: flow-engine-framework
description: 流程服务层，封装流程的创建、审批、撤销、催办等核心业务操作
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - FlowService
  - WorkflowService
  - FlowRecordService
  - FlowRecordSaveService
  - FlowActionService
  - FlowCreateService
  - FlowDelayTriggerService
  - FlowDetailService
  - FlowProcessNodeService
  - FlowRevokeService
  - FlowUrgeService
  - OperatorAssignmentService
  - WorkflowGroovyScriptUtils
content_hash: 48847288af1a4551b8327e398d01a8ec4de643dce1c049b90dcdc8ad0134df3b
---

## 解决什么问题

提供流程引擎的业务服务层，将复杂的流程操作封装为高内聚的服务接口，解决以下问题：

- **流程操作入口**：`FlowService` 作为流程审批操作的统一入口，支持创建、审批、撤销、催办
- **流程设计管理**：`WorkflowService` 管理流程定义的 CRUD 和版本操作
- **职责分离**：每种操作由独立的 Service 实现（如 `FlowActionService`、`FlowCreateService`），保持单一职责
- **线程安全**：每次操作前通过 `FlowOperatorLocalThreadCache.clear()` 清理线程缓存

## 如何使用

### FlowService — 流程审批入口

| 方法 | 说明 |
|------|------|
| `create(FlowCreateRequest)` | 创建流程实例，返回流程 ID |
| `action(FlowActionRequest)` | 执行审批动作（通过/驳回/转办等） |
| `revoke(FlowRevokeRequest)` | 撤销流程 |
| `urge(FlowUrgeRequest)` | 催办 |
| `detail(FlowDetailRequest)` | 查询流程详情 |
| `processNodes(FlowProcessNodeRequest)` | 查询流程节点记录 |

### WorkflowService — 流程设计管理

| 方法 | 说明 |
|------|------|
| `saveWorkflow(...)` | 保存流程定义 |
| `importWorkflow(...)` | 导入流程定义 |
| 版本管理 | 启用/禁用版本 |

### 内部服务分工

| 服务 | 职责 |
|------|------|
| `FlowCreateService` | 创建流程实例，初始化节点和记录 |
| `FlowActionService` | 执行审批动作，驱动流程流转 |
| `FlowRevokeService` | 撤销流程实例 |
| `FlowDetailService` | 查询流程详情和当前状态 |
| `FlowProcessNodeService` | 查询流程节点执行记录 |
| `FlowDelayTriggerService` | 处理延迟触发任务 |
| `FlowUrgeService` | 处理催办逻辑 |
| `FlowRecordSaveService` | 保存流程记录和待办合并关系 |
| `OperatorAssignmentService` | 操作者分配服务 |

## 使用实例

```java
// 通过 Spring 注入获取 FlowService
@Autowired
private FlowService flowService;

// 1. 创建流程
FlowCreateRequest createRequest = new FlowCreateRequest();
createRequest.setWorkCode("leave-request");
createRequest.setOperatorId(userId);
long flowId = flowService.create(createRequest);

// 2. 审批通过
FlowActionRequest actionRequest = new FlowActionRequest();
actionRequest.setFlowId(flowId);
actionRequest.setActionType("PASS");
actionRequest.setOperatorId(userId);
ActionResponse response = flowService.action(actionRequest);

// 3. 撤销流程
FlowRevokeRequest revokeRequest = new FlowRevokeRequest();
revokeRequest.setFlowId(flowId);
flowService.revoke(revokeRequest);

// 4. 查询流程详情
FlowDetailRequest detailRequest = new FlowDetailRequest();
detailRequest.setFlowId(flowId);
FlowContent content = flowService.detail(detailRequest);
```
