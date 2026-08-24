# REST API

引擎提供两层 REST API：命令操作（`flow-engine-starter-api`）与查询（`flow-engine-starter-query`）。响应统一使用外部框架的 `Response` / `SingleResponse<T>` / `MultiResponse<T>` 包装。

## 1. 操作人解析规则（两个 Controller 通用）

`FlowRecordController` 与 `FlowRecordQueryController` 均实现「mockKey 分流 + operatorId 覆盖」：

```
mockKey 请求参数 → 存在且有效 → 使用对应 MockInstance 的 FlowService（内存沙箱）
                → 不存在/失效 → 使用生产 FlowService

operatorId 请求参数 → 存在 → 使用该 id 作为当前操作人
                   → 不存在 → UserContext.getInstance().current() 强转 IFlowOperator 取 userId
```

- `mockKey` 经 `MockInstanceFactory.getInstance().getMockInstance(key)` 解析（见 [Mock 模式](./mock-mode.md)）。
- `operatorId` 覆盖便于无登录态集成与测试（如 curl 直接带 `?operatorId=1`）。

## 2. 命令操作（`/api/cmd/record`）

`flow-engine-starter-api/src/main/java/com/codingapi/flow/api/controller/FlowRecordController.java`

| 方法 | 路径 | 请求体/参数 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/api/cmd/record/detail` | query `id` | `SingleResponse<FlowContent>` | 流程详情（含当前节点/表单/动作/历史/待办列表） |
| POST | `/api/cmd/record/processNodes` | `FlowProcessNodeRequest` | `MultiResponse<ProcessNode>` | 流程流转节点预览 |
| POST | `/api/cmd/record/create` | `FlowCreateRequest` | `SingleResponse<Long>` | 发起流程，返回首条记录 id |
| POST | `/api/cmd/record/action` | `FlowActionRequest` | `SingleResponse<ActionResponse>` | 执行动作（通过/拒绝/加签/委派/退回/转办/自定义/保存） |
| POST | `/api/cmd/record/subProcess/reset` | `FlowSubProcessResetRequest` | `Response` | 子流程数据重置（独立接口，需子流程节点开启重置能力） |
| POST | `/api/cmd/record/urge` | `IdRequest` | `Response` | 催办 |
| POST | `/api/cmd/record/revoke` | `IdRequest` | `Response` | 撤销 |
| POST | `/api/cmd/record/delete` | `IdRequest` | `Response` | 删除（仅未流转实例，开始节点） |

## 3. 流程设计（`/api/cmd/workflow`）

`flow-engine-starter-api/src/main/java/com/codingapi/flow/api/controller/WorkflowController.java`

| 方法 | 路径 | 请求体/参数 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/api/cmd/workflow/meta` | query `id`（workCode） | `SingleResponse<WorkflowMeta>` | 流程元数据（子流程配置下拉使用） |
| GET | `/api/cmd/workflow/load` | query `id` | `SingleResponse<JSONObject>` | 加载流程 JSON |
| POST | `/api/cmd/workflow/save` | `JSONObject`（Flow JSON） | `Response` | 保存流程（带 `versionName` 时创建版本） |
| POST | `/api/cmd/workflow/create` | — | `SingleResponse<JSONObject>` | 创建空流程（开始+结束节点） |
| POST | `/api/cmd/workflow/import` | `{ "file": "<flow json>" }` | `SingleResponse<String>` | 导入流程，返回 workId |
| GET | `/api/cmd/workflow/export` | query `id` | 文件下载 | 导出流程 JSON |
| POST | `/api/cmd/workflow/remove` | `IdRequest` | `Response` | 删除流程 |
| POST | `/api/cmd/workflow/changeState` | `IdRequest` | `Response` | 启用/停用流程 |
| POST | `/api/cmd/workflow/changeVersion` | `IdRequest`（versionId） | `Response` | 切换当前版本 |
| POST | `/api/cmd/workflow/deleteVersion` | `IdRequest`（versionId） | `Response` | 删除版本 |
| POST | `/api/cmd/workflow/updateVersionName` | `WorkflowUpdateVersionNameRequest` | `Response` | 更新版本名 |
| POST | `/api/cmd/workflow/create-node` | `NodeCreateRequest{ type }` | `SingleResponse<Map>` | 创建设计器节点（块节点自动带 2 个分支） |
| POST | `/api/cmd/workflow/create-custom-action` | — | `SingleResponse<Map>` | 创建自定义动作默认配置 |
| POST | `/api/cmd/workflow/mock` | — | `SingleResponse<String>` | 创建 Mock 沙箱，返回 mockKey（需流程管理员） |
| POST | `/api/cmd/workflow/cleanMock` | `IdRequest` | `Response` | 清理 Mock 沙箱 |

## 4. 查询（`/api/query/record`）

`flow-engine-starter-query/src/main/java/com/codingapi/flow/query/controller/FlowRecordQueryController.java`

| 方法 | 路径 | 参数 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/api/query/record/list` | `SearchRequest`（current/pageSize） | `MultiResponse<FlowRecordContent>` | 全部流程记录（按 id 降序） |
| GET | `/api/query/record/todo` | `SearchRequest` | `MultiResponse<FlowRecordContent>` | 我的待办 |
| GET | `/api/query/record/notify` | `SearchRequest` | `MultiResponse<FlowRecordContent>` | 我的抄送 |
| GET | `/api/query/record/done` | `SearchRequest` | `MultiResponse<FlowRecordContent>` | 我的已办 |

`FlowRecordQueryService`（`flow-engine-framework/.../query/FlowRecordQueryService.java`）：

```java
public interface FlowRecordQueryService {
    Page<FlowRecordContent> findAll(PageRequest request);
    Page<FlowRecordContent> findTodoRecordPage(long userId, PageRequest request);
    Page<FlowRecordContent> findNotifyRecordPage(long userId, PageRequest request);
    Page<FlowRecordContent> findDoneRecordPage(long userId, PageRequest request);
}
```

## 5. 流程查询（`/api/query/workflow`）

`flow-engine-starter-query/src/main/java/com/codingapi/flow/query/controller/WorkflowQueryController.java`（直接基于 infra 的 JPA 仓储）

| 方法 | 路径 | 参数 | 响应 | 说明 |
|---|---|---|---|---|
| GET | `/api/query/workflow/list` | `SearchRequest` | `MultiResponse<WorkflowEntity>` | 流程列表 |
| GET | `/api/query/workflow/options` | — | `MultiResponse<WorkflowOption>` | 流程下拉选项 |
| GET | `/api/query/workflow/versions` | query `id`（workId） | `MultiResponse<WorkflowVersionOption>` | 流程版本列表 |

## 6. 节点视图脚本（`/api/view/javascript`）

`flow-engine-starter-api/src/main/java/com/codingapi/flow/api/controller/NodeViewJavaScriptController.java`

提供节点视图 JS 的读写接口（`NodeViewJavaScript` 持久化经 `NodeViewJavaScriptRepository`）。

## 7. Groovy 脚本接口（`/api/groovy-script`）

由外部依赖 `springboot-starter-script` 自动注册（`GroovyScriptController`）：

| 方法 | 路径 | 请求体 | 说明 |
|---|---|---|---|
| POST | `/api/groovy-script/compile` | `{ cache, script }` | 编译脚本 |
| GET | `/api/groovy-script/getScript?key=` | query | 获取脚本内容 |
| GET | `/api/groovy-script/getMetadata?key=` | query | 获取脚本元数据（`GroovyMetadata`） |
| POST | `/api/groovy-script/save` | `{ key, script }` | 保存脚本（临时 → 正式） |

## 8. 请求/响应结构

### `FlowCreateRequest`

```json
{
  "workCode": "leave_flow",
  "actionId": "开始节点 SAVE 动作 id",
  "formData": { "days": 3, "reason": "年假" },
  "operatorId": 1001,
  "operatorSelectMap": { "approvalNodeId": [1002, 1003] }
}
```

- `operatorSelectMap`：发起人设定操作人（`OperatorLoadStrategy.INITIATOR_SELECT` 节点），key 为节点 id，value 为操作人 id 列表。

### `FlowActionRequest`

```json
{
  "recordId": 123,
  "formData": { "days": 3 },
  "advice": {
    "actionId": "PASS 动作 id",
    "advice": "同意",
    "signKey": "签名",
    "forwardOperatorIds": [1002],
    "backNodeId": "退回目标节点 id",
    "operatorSelectMap": {}
  }
}
```

- `actionId` 决定执行动作（PASS/REJECT/ADD_AUDIT/DELEGATE/RETURN/TRANSFER/CUSTOM/SAVE）。
- `forwardOperatorIds`：加签/委派/转办的目标操作人。
- `backNodeId`：退回动作的目标节点（仅 Start/Approval/Handle 可退回）。

### `FlowSubProcessResetRequest`（子流程数据重置，独立接口）

```json
{
  "recordId": 456,
  "resetInstanceProcessIds": ["选中重建的子流程实例流程id"],
  "advice": "重置说明"
}
```

- 重置不属于审批动作，不经过 `/action` 接口；仅当子流程节点开启 `resettable` 能力且当前待办位于其下游时可调用。
- 无需指定子流程节点：由选中实例流程id定位其所属聚合组，选中实例须同属一个已放行且未取代的聚合组。
- 流程详情 `FlowContent.resetSubProcess` 标识字段表明当前记录是否具备该能力，前端据此提供交互。

### `ActionResponse`

```json
{
  "responseType": "MANUAL_NODE_SELECT | OPERATOR_SELECT",
  "options": [
    { "id": "nodeId", "name": "节点名", "type": "APPROVAL", "display": true,
      "operators": [{ "userId": 1, "name": "张三", "flowManager": false }],
      "maxOperatorCount": -1 }
  ]
}
```

- `MANUAL_NODE_SELECT`：通过后需要选择人工节点分支。
- `OPERATOR_SELECT`：下一节点为发起人设定操作人（`INITIATOR_SELECT`），需要选择操作人（`operators` = 可选范围，`maxOperatorCount` = 最大可选人数，-1 不限制）。

### `FlowContent`（详情响应，字段摘要）

```
recordId / processId / workId / workTitle / workDescription / createTime / workCode
view / code / viewTitle / nodeName / nodeId / nodeType
title（节点标题） / adviceRequired / signRequired / adviceHidden
form（FlowForm 元数据） / fieldPermissions（字段权限）
todos（当前节点待办 Body 列表） / actions / actionList
mergeable / createOperator / currentOperator / flowState / recordState
histories（审批历史） / nodes（NodeOption 可操作节点） / revoke / urge
resetSubProcess（子流程重置能力标识：当前待办位于开启重置能力、已汇聚完成的子流程下游时为 true）
```

### `FlowRecordContent`（列表响应，字段摘要）

```
processId / workflowRuntimeId / workTitle / workCode / nodeId / nodeType / nodeName
title / readTime / currentOperatorId / currentOperatorName
submitOperatorId / submitOperatorName / createTime
createOperatorId / createOperatorName / todoKey / margeCount / mergeable
recordId / timeoutTime / recordState / flowState / notify
```

## 9. 响应包装

| 类 | 结构 |
|---|---|
| `Response` | `{ success: boolean, errCode, errMessage }` |
| `SingleResponse<T>` | `{ data: T, success, ... }` |
| `MultiResponse<T>` | `{ data: T[], total, success, ... }` |

异常统一为 `LocaleMessageException` 体系（`FlowException` 的 5 个子类，见 [扩展点](./extension-points.md#异常体系)），错误码见各异常工厂方法。