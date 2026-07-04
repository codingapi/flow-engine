# Flow Engine 使用手册

本手册面向后端与前端开发者，介绍集成完成后如何使用 Flow Engine 进行流程设计与流转操作。

## 1. 整体概念

### 1.1 流程定义 vs 流程实例

- **流程定义（Workflow）**：可视化的审批流设计图，由节点和连线组成，通过 `WorkflowController` 的 `/api/cmd/workflow/**` 接口管理。流程定义支持多版本（`WorkflowVersion`），可切换/启用/停用版本。
- **流程实例（FlowRecord）**：一次具体的审批申请，运行时产生。一条流程实例由若干 `FlowRecord`（执行记录）+ `FlowTodoRecord`（待办）+ `FlowTodoMerge`（待办合并关系）组成。

### 1.2 关键术语

| 术语 | 说明 |
|------|------|
| 节点（Node） | 流程图中的步骤，共 19 种类型 |
| 动作（Action） | 在节点上可执行的操作，共 8 种类型 |
| 策略（Strategy） | 驱动节点和流程行为的规则 |
| 操作者（Operator） | 流程参与用户，实现 `IFlowOperator` |
| 表单数据（formData） | 流程运行时的业务数据，`Map<String,Object>` |
| 审批意见（advice） | 动作执行时附带的意见、签名、退回节点等 |
| workCode | 流程定义的编码，发起流程时用于定位流程定义 |

## 2. 节点类型（19 种）

| 类型枚举 | 中文名 | 说明 |
|----------|--------|------|
| `START` | 开始节点 | 流程起点，发起人 |
| `END` | 结束节点 | 流程终点 |
| `APPROVAL` | 审批节点 | 人工审批，最常用节点 |
| `HANDLE` | 办理节点 | 人工办理（无同意/拒绝语义） |
| `CONDITION` | 条件控制 | 条件分支父节点 |
| `CONDITION_BRANCH` | 条件分支 | 满足条件走该分支 |
| `CONDITION_ELSE_BRANCH` | 条件 else 分支 | 都不满足时走该分支 |
| `INCLUSIVE` | 包容控制 | 包容分支父节点（多分支可同时满足） |
| `INCLUSIVE_BRANCH` | 包容分支 | 包容网关的一个分支 |
| `INCLUSIVE_ELSE_BRANCH` | 包容 else 分支 | 默认包容分支 |
| `PARALLEL` | 并行控制 | 并行分支父节点（所有分支同时执行） |
| `PARALLEL_BRANCH` | 并行分支 | 并行网关的一个分支 |
| `MANUAL` | 人工控制 | 人工选择走哪条分支 |
| `MANUAL_BRANCH` | 人工分支 | 供人工选择的分支 |
| `ROUTER` | 路由节点 | 由 Groovy 脚本决定下一节点 |
| `NOTIFY` | 抄送节点 | 抄送给指定人，不阻塞流程 |
| `DELAY` | 延迟节点 | 延时执行，由延迟任务驱动 |
| `SUB_PROCESS` | 子流程节点 | 触发一条独立的子流程 |
| `TRIGGER` | 触发节点 | 触发外部动作/事件 |

> 节点类型常量定义为 `XxxNode.NODE_TYPE = NodeType.XXX.name()`，例如 `ApprovalNode.NODE_TYPE` = `"APPROVAL"`。

## 3. 动作类型（8 种）

发起流程和审批时通过 `actionId` 指定执行的动作。动作类型枚举 `ActionType`：

| 枚举 | 中文 | 触发行为 | 必填参数 |
|------|------|---------|---------|
| `PASS` | 通过 | 流程继续向下流转 | 表单（按字段权限） |
| `REJECT` | 拒绝 | 按拒绝配置结束流程或退回 | 可能需审批意见 |
| `RETURN` | 退回 | 退回到指定历史节点 | `backNodeId` |
| `TRANSFER` | 转办 | 转给指定人审批，原审批人不再处理 | `forwardOperatorIds` |
| `DELEGATE` | 委派 | 委派给他人审批，其完成后回到自己 | `forwardOperatorIds` |
| `ADD_AUDIT` | 加签 | 增加审批人会签 | `forwardOperatorIds` |
| `SAVE` | 保存 | 暂存表单，不触发流转 | 无 |
| `CUSTOM` | 自定义 | 自定义按钮，由脚本驱动 | 依脚本 |

## 4. REST API 总览

### 4.1 命令操作（`/api/cmd/**`）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/cmd/workflow/create` | 创建空流程定义（返回 JSON） |
| POST | `/api/cmd/workflow/save` | 保存流程定义（含节点/连线） |
| GET | `/api/cmd/workflow/load?id=` | 加载流程定义 JSON |
| GET | `/api/cmd/workflow/meta?id=` | 获取流程元数据 |
| POST | `/api/cmd/workflow/remove` | 删除流程定义 |
| POST | `/api/cmd/workflow/changeState` | 启用/停用流程 |
| POST | `/api/cmd/workflow/changeVersion` | 切换当前启用版本 |
| POST | `/api/cmd/workflow/deleteVersion` | 删除某版本 |
| POST | `/api/cmd/workflow/updateVersionName` | 修改版本名 |
| POST | `/api/cmd/workflow/import` | 导入流程定义（JSON 字符串） |
| GET | `/api/cmd/workflow/export?id=` | 导出流程定义为 JSON 文件 |
| POST | `/api/cmd/workflow/create-node` | 创建单个节点（按 `type`） |
| POST | `/api/cmd/workflow/create-custom-action` | 创建自定义动作模板 |
| POST | `/api/cmd/workflow/mock` | 创建 Mock 实例（测试用） |
| POST | `/api/cmd/workflow/cleanMock` | 清理 Mock 实例 |
| POST | `/api/cmd/record/create` | **发起流程实例** |
| POST | `/api/cmd/record/action` | **执行审批动作** |
| POST | `/api/cmd/record/revoke` | 撤销流程实例 |
| POST | `/api/cmd/record/delete` | 删除流程实例（仅开始节点） |
| POST | `/api/cmd/record/urge` | 催办 |
| GET | `/api/cmd/record/detail` | 流程详情 |
| POST | `/api/cmd/record/processNodes` | 流程节点列表 |

### 4.2 查询操作（`/api/query/**`）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/query/record/list` | 全部流程记录（分页） |
| GET | `/api/query/record/todo` | 我的待办 |
| GET | `/api/query/record/done` | 我的已办 |
| GET | `/api/query/record/notify` | 我的抄送 |
| GET | `/api/query/workflow/list` | 流程定义列表 |
| GET | `/api/query/workflow/options` | 流程定义下拉选项 |
| GET | `/api/query/workflow/versions` | 流程版本列表 |

### 4.3 统一响应格式

所有接口返回 `com.codingapi.springboot.framework` 的响应封装：

```jsonc
// SingleResponse
{ "code": "0000", "msg": "success", "data": {} }

// MultiResponse（分页）
{ "code": "0000", "msg": "success", "data": [], "total": 0, "current": 0, "pageSize": 20 }

// Response（无返回体）
{ "code": "0000", "msg": "success" }
```

### 4.4 请求查询约定

- 分页：`SearchRequest` 提供 `current`（页码，0 开始）与 `pageSize`。
- 主键：`IdRequest` 支持 `id` 字符串形式（`getStringId()` / `getLongId()`）。流程 ID 既可能是数字 `recordId`，也可能是字符串 `workCode`，引擎内部通过 `FlowDetailRequest.isCreateWorkflow()` 判断（非纯数字视为 `workCode`）。

## 5. 调试技巧

命令类 Controller 在每个请求中支持两个可选查询参数，便于无登录态调试：

| 参数 | 作用 |
|------|------|
| `operatorId` | 直接指定当前操作者 ID，覆盖 `UserContext` |
| `mockKey` | 路由到对应 Mock 实例的 `FlowService`/`FlowRecordQueryService`，用于脱离真实数据库测试 |

示例：

```
GET /api/query/record/todo?operatorId=1001&current=0&pageSize=20
POST /api/cmd/record/create?operatorId=1001
```

Mock 实例通过 `POST /api/cmd/workflow/mock` 创建，返回 `mockKey`，后续请求带上即可。

## 6. 完整业务流程：发起并审批

下面以一个"请假审批"流程为例，演示完整的端到端调用。

### 6.1 第一步：设计并保存流程定义

```bash
# 创建空流程定义（返回包含默认开始/结束节点的 JSON）
POST /api/cmd/workflow/create

# 按需调用 create-node 添加 APPROVAL 等节点并设置连线，
# 最终把完整 JSON 提交保存
POST /api/cmd/workflow/save
Content-Type: application/json

{
  "id": "leave-flow",
  "title": "请假审批",
  "versionName": "v1.0",
  "nodes": [ ... ],
  "edges": [ ... ]
}
```

保存时若携带 `versionName`，引擎会创建一个新版本并设为当前版本。

### 6.2 第二步：发起流程

```bash
POST /api/cmd/record/create
Content-Type: application/json

{
  "workCode": "leave-flow",
  "formData": {
    "days": 3,
    "reason": "事假"
  },
  "actionId": "action_pass_xxx",
  "operatorSelectMap": null
}
```

请求体 `FlowCreateRequest` 字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `workCode` | String | 流程定义编码 |
| `formData` | `Map<String,Object>` | 表单数据，必填非空 |
| `actionId` | String | 发起时执行的动作 ID（通常为开始节点的通过动作） |
| `operatorId` | long | 操作者（由 Controller 自动从 `operatorId` 参数或 `UserContext` 注入，前端可不传） |
| `parentRecordId` | long | 父流程 ID，子流程场景由系统自动赋值，前端不传 |
| `operatorSelectMap` | `Map<String,List<Long>>` | 发起人手动指定某节点（`INITIATOR_SELECT` 模式）的审批人，键为节点 ID |

返回：`SingleResponse<Long>` —— 创建出的流程记录 ID（`recordId`）。

### 6.3 第三步：执行审批动作

```bash
POST /api/cmd/record/action
Content-Type: application/json

{
  "recordId": 1001,
  "formData": { "days": 3, "reason": "事假", "comment": "同意" },
  "advice": {
    "actionId": "action_pass_xxx",
    "advice": "同意请假",
    "signKey": null,
    "manualNodeId": null,
    "backNodeId": null,
    "forwardOperatorIds": null,
    "operatorSelectMap": null
  }
}
```

请求体 `FlowActionRequest` 字段说明：

| 字段 | 类型 | 说明 |
|------|------|------|
| `recordId` | long | 流程记录 ID |
| `formData` | `Map<String,Object>` | 表单数据，必填非空 |
| `advice` | `FlowAdviceBody` | 审批意见 |

`advice`（`FlowAdviceBody`）字段说明：

| 字段 | 类型 | 适用动作 | 说明 |
|------|------|---------|------|
| `actionId` | String | 所有 | 要执行的动作 ID |
| `advice` | String | PASS/REJECT | 审批意见文字 |
| `signKey` | String | PASS | 签名 key（节点要求签名时必填） |
| `backNodeId` | String | RETURN | 退回目标节点 ID |
| `forwardOperatorIds` | `List<Long>` | TRANSFER/DELEGATE/ADD_AUDIT | 转办/委派/加签人员 |
| `manualNodeId` | String | MANUAL | 人工选择的目标分支节点 ID |
| `operatorSelectMap` | `Map<String,List<Long>>` | PASS | 为后续节点指定审批人 |

返回：`SingleResponse<ActionResponse>`。当流程遇到人工选择节点或需要操作人指定审批人时，`ActionResponse` 会返回 `responseType` 与可选节点列表 `options`，前端据此让用户选择后再次调用 `/action`：

```jsonc
{
  "data": {
    "responseType": "MANUAL_NODE_SELECT",   // 或 OPERATOR_SELECT
    "options": [ { "nodeId": "...", "title": "分支A" }, ... ]
  }
}
```

### 6.4 第四步：流转过程中的其他操作

```bash
# 撤销（发起人可撤销自己刚发起的流程）
POST /api/cmd/record/revoke   { "id": "1001" }

# 催办（催促当前待办人）
POST /api/cmd/record/urge     { "id": "1001" }

# 删除（仅删除位于开始节点、尚未流转的流程）
POST /api/cmd/record/delete   { "id": "1001" }

# 查看详情
GET  /api/cmd/record/detail?id=1001
```

### 6.5 第五步：查询待办/已办

```bash
# 我的待办
GET /api/query/record/todo?current=0&pageSize=20

# 我的已办
GET /api/query/record/done?current=0&pageSize=20

# 我的抄送
GET /api/query/record/notify?current=0&pageSize=20
```

## 7. 常见场景

### 7.1 退回指定节点

在审批节点调用退回动作：

```json
{
  "recordId": 1001,
  "formData": { "...": "..." },
  "advice": {
    "actionId": "action_return_xxx",
    "advice": "资料不全，请补充",
    "backNodeId": "node_start_001"
  }
}
```

退回约束（`ActionManager.verifySession`）：

- `backNode` 不能为 `null`
- `backNode` 不能是结束节点
- `backNode` 不能是当前节点
- `backNode` 不能是当前节点的后续节点
- `backNode` 类型必须是 `START` / `APPROVAL` / `HANDLE`

### 7.2 转办 / 委派 / 加签

三者都需提供 `forwardOperatorIds`：

- **转办（TRANSFER）**：流程转给他人，自己不再审批。
- **委派（DELEGATE）**：委派他人审批，其通过后流程回到自己。
- **加签（ADD_AUDIT）**：增加会签人，会签完成后继续流转。

### 7.3 人工分支选择

当流程进入 `MANUAL` 节点，调用 `/action` 会被引擎拦截并返回 `MANUAL_NODE_SELECT` 响应及可选分支列表。前端让用户选择分支后，把选中的 `nodeId` 放入 `advice.manualNodeId` 再次提交即可。

### 7.4 发起人指定审批人

对于配置为 `INITIATOR_SELECT`（发起人选择）模式的节点，发起流程时在 `operatorSelectMap` 中传入节点 ID 到审批人 ID 列表的映射；或审批通过时在 `advice.operatorSelectMap` 为后续节点指定。

### 7.5 子流程

`SUB_PROCESS` 节点触发时，引擎自动以当前流程为父（`parentRecordId`）发起一条独立的子流程实例。子流程结束后回到父流程继续流转。

## 8. 事件订阅

业务系统通过实现 `IHandler<E>` 订阅事件以做副作用处理（发通知、写日志、同步外部系统）：

```java
@Slf4j
@Component
public class MyFlowRecordTodoEventHandler implements IHandler<FlowRecordTodoEvent> {
    @Override
    public void handler(FlowRecordTodoEvent event) {
        // event 携带 FlowRecord 与 mock 标记
        // 例如：向待办人推送 IM 消息
    }
}
```

7 种事件见《集成手册》"6.1 事件订阅"。事件携带 `FlowRecord` 及是否 `mock` 标记，可据此区分真实流程与测试流程。

## 9. 流程记录生命周期

理解三类记录的关系，有助于排查流转异常：

| 记录 | 职责 | 状态判定 |
|------|------|---------|
| `FlowRecord` | 流程执行记录 | `isTodo()` / `isDone()` / `isFinish()` |
| `FlowTodoRecord` | 待办记录 | 待办 |
| `FlowTodoMerge` | 待办合并关系 | 开启记录合并时产生 |

- **保存顺序**：`FlowRecordSaveService.saveAll()` 按 `saveRecords → saveTodoMargeRecords → removeTodoMergeRecords` 顺序执行，保证三者一致。
- **删除顺序**：`FlowRecordService.deleteFlowRecord()` 按 `合并关系 → 待办 → 流程记录` 清理。

所有流程操作走 `FlowService`（`@Transactional`），每个操作前清理 `FlowOperatorLocalThreadCache`，单次操作对应独立的 `FlowXxxService`（Create/Action/Revoke/Delete/Urge/Detail）。

## 10. 常见问题

**Q1：发起流程报 `workCode` 必填？**
`FlowCreateRequest.verify()` 要求 `workCode`、`formData`（非空）、`operatorId`、`actionId` 均必填。检查是否遗漏 `actionId`（开始节点的通过动作 ID）。

**Q2：审批时报 `advice` 必填？**
`FlowActionRequest.verify()` 要求 `recordId > 0`、`formData` 非空、`advice` 非空，且 `advice.verify()` 会进一步校验动作专属参数（如退回的 `backNodeId`、转办的 `forwardOperatorIds`）。

**Q3：如何区分 `recordId` 与 `workCode`？**
引擎通过 `FlowDetailRequest.isCreateWorkflow()` 判断：`id` 为纯数字视为 `recordId`，否则视为 `workCode`。

**Q4：操作者是当前登录用户，怎么注入？**
命令类 Controller 自动从 `UserContext.getInstance().current()` 取当前用户的 `userId`。需在鉴权过滤器中写入 `UserContext`。调试时可加 `?operatorId=1001` 参数覆盖。

**Q5：表单字段校验规则？**
流程表单仅校验必填字段，多余字段保留原值（参见近期提交 `59e0f15`）。字段级权限由 `FormFieldPermissionStrategy` 驱动。

## 11. 进一步阅读

- 集成步骤：[集成手册](./integration-manual.md)
- 能力清单（可复用能力，**禁止重复实现**）：[capabilities/index](../capabilities/index.md)
- 开发规范：[conventions/index](../conventions/index.md)
- 节点策略详解：[capabilities/flow-engine-framework/node-strategy](../capabilities/flow-engine-framework/node-strategy.md)
- 动作体系：[capabilities/flow-engine-framework/action](../capabilities/flow-engine-framework/action.md)
- 脚本运行时：[capabilities/flow-engine-framework/script](../capabilities/flow-engine-framework/script.md)
