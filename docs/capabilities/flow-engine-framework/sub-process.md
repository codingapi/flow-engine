---
name: flow-engine-framework/sub-process
module: flow-engine-framework
description: 子流程聚合执行能力，支持批量创建、主流程等待、脚本结果判定与异常跳转
status: 已实现
scope: 后端
source: 项目自有
import: "com.codingapi.flow:flow-engine-framework"
symbols:
  - SubProcessNode
  - SubProcessStrategy
  - SubProcessRecord
  - SubProcessContext
  - SubProcessRepository
  - SubProcessScript
  - SubProcessResultScript
  - FlowSubProcessResultService
  - FlowSubProcessResetService
  - FlowSubProcessResetRequest
  - FlowSubProcessResetEvent
  - FlowSession.findSubProcessRecords
  - ProcessNode.SubProcessBody
---

## 解决什么问题

提供子流程节点的聚合执行与结果确认能力：

- 子流程创建脚本可返回单个 `FlowCreateRequest` 或多个请求的集合。
- 主流程在子流程节点不产生普通节点记录，由 `SubProcessRecord` 记录本次执行的总数、子流程实例和聚合状态。
- 每个子流程结束时都执行结果脚本；脚本返回 `true` 时恢复主流程，全部结束仍返回 `false` 时执行异常节点跳转。
- 聚合状态一旦放行或异常即不再重复恢复，支持按数量、占比或业务数据提前放行。

## 脚本上下文

结果脚本可使用以下方法：

| 方法 | 说明 |
|------|------|
| `request.findSubProcessRecords(nodeId)` | 返回指定子流程节点中已结束实例的最终 `FlowRecord` |
| `request.getSubProcessTotal()` | 返回本次子流程节点创建的实例总数 |
| `request.getCurrentSubProcessRecord()` | 返回本次触发判定的子流程最终记录 |

`FlowSession.findSubProcessRecords(nodeId)` 也可在子流程后续节点的其他脚本中使用。该方法每个子流程只返回一条最终记录；需要完整审批历史时，再根据返回记录的 `processId` 查询。

## 默认判定

```groovy
def run(request){
    return request.findSubProcessRecords(request.getCurrentNode().getId()).size()
        == request.getSubProcessTotal()
}
```

## 持久化与并发

`flow-engine-starter-infra` 使用 `t_flow_sub_process_record` 表保存聚合记录。子流程结束查询使用悲观写锁，实体同时使用版本号，避免多个子流程并发结束时重复恢复主流程。

## 流程节点记录展示

`FlowProcessNodeService` 会把 `SubProcessRecord` 作为真实的控制节点执行记录合并到主流程节点视图中，不创建或伪造主流程 `FlowRecord`：

- `WAITING` 映射为 `PROCESSING`，并以子流程节点为起点继续预览后续节点。
- `PASSED` 映射为 `PASS`，`ERROR` 映射为 `ERROR`。
- `ProcessNode.subProcess` 返回聚合记录 ID、总数、完成数、时间及各子流程实例的记录 ID、流程 ID 和状态。
- 子流程实例同时保存创建时的流程名称快照；历史聚合 JSON 没有名称时，节点查询会根据开始记录批量回填。
- 子流程记录通过 `parentRecordId` 锚定到触发它的主流程记录，兼容并行分支及节点重复执行顺序。
- 按比例提前放行后，剩余子流程结束仍会更新实例状态，但不会再次判定或恢复主流程。
- 子流程最终表单与完整审批历史不直接嵌入节点视图，需要根据实例记录 ID 或 `processId` 二次查询。

## 子流程查看主流程历史

`SubProcessStrategy.showParentProcessRecords` 控制子流程参与人能否在节点记录中查看主流程历史，默认关闭（存量历史流程定义同样保持关闭，不自动开启）：

- 开启后，查询子流程节点记录会递归拼接可见的祖先流程历史、对应子流程聚合节点以及当前子流程实际记录。
- 主流程历史严格沿持久化 `fromId` 链回溯，仅展示到触发当前子流程为止的实际路径，不执行主流程未来节点推演，也不展开无关分支。
- 当前子流程仅展示已产生的记录和结束节点，不增加“汇入主流程”虚拟记录。
- 拼接的祖先流程节点统一返回 `ProcessNode.parentProcessRecord=true`，当前子流程节点为 `false`，供展示层区分记录来源。
- 开关开启时主流程历史包含审批人和审批意见，因此应由流程设计者按数据可见性要求显式配置。

## 子流程数据重置（issue #219）

子流程汇聚放行后、下游审批发现数据有误时，可对**已完成的子流程聚合记录**执行退回重走。重置是**独立接口**（不属于常规审批动作），能力完全由业务配置控制：

### 能力开关（子流程节点配置）

`SubProcessStrategy` 新增 `resettable` 布尔配置，**默认关闭**。仅当子流程节点开启该能力时，其汇聚完成后的下游待办记录才具备重置能力，`/api/cmd/record/subProcess/reset` 接口才允许调用；未开启时接口直接拒绝。历史流程定义无该字段时按关闭处理（`fromMap` 兼容）。

### 接口与定位方式

- 请求 `FlowSubProcessResetRequest`：`recordId`（当前操作的待办记录）+ `resetInstanceProcessIds`（选中重建的实例流程id）+ `advice`（重置说明）。
- **不需要指定子流程节点**：由选中实例的流程id定位其所属聚合组（全部选中实例须同属一个未取代聚合组），重置始终是整个子流程**从头重走一次**——重建实例经由子流程节点配置的创建脚本与自动提交策略重新发起，不跳转、不迁移、不自动跳过任何历史数据。
- 前置约束：当前记录为待办且操作人匹配；目标聚合组已放行（PASSED）、未被取代、全部实例已结束；当前流程无等待中聚合组；当前记录位于**锁定合并节点**（见下）。

### 锁定合并节点（复杂下游拓扑支持）

合并节点 = 子流程放行恢复后，锚点记录之后**首个产生业务记录的节点**。恢复遍历以锚点为来源记录，合并点首代记录均满足 `fromId == 锚点id`，据此从真实执行数据判定（抄送记录不参与判定）：

- 串联 / 条件分支 / 触发节点 / 抄送直通：合并节点唯一——条件分支取**实际命中分支**的首个节点，触发与抄送节点不阻断、不产生合并点；
- 并行 / 包容分支扇出：多个分支首节点记录并存时，按记录产生顺序**锁定第一个分支的节点**为合并节点，兄弟分支不可重置；
- **重置仅允许在锁定合并节点的待办记录上发起**，合并节点之后的更深层节点不可重置（重置语义为退回子流程重走，入口固定在汇聚处）。

重置执行时同步清除该流程实例的循环触发标记（`LoopTriggerTraceContext.clearByProcess`），保证重走再次经过下游节点（如抄送）时不被被动式环检测误判为循环。

### 重置语义

- **聚合组建模**：旧聚合组标记 `superseded`（聚合状态保持不变，历史记录保留有效可查），新建一条聚合组取代其成为当前有效数据。新组由两类实例组成：
  - **继承实例**（未选中，`Instance.inherited=true`）：沿用原实例的流程id与最终状态，不重新执行；
  - **重建实例**（选中）：基于旧实例的启动记录反推创建请求（沿用原子流程定义、表单数据、流程标题与原发起人），创建全新子流程（新流程id），`Instance.sourceProcessId` 记录其替换的旧实例流程id，供订阅方完成旧 → 新映射。
- **锚点不变**：新组沿用旧组的 `parentRecordId`（原触发记录），保证结果判定后主流程恢复位置不变。
- **记录链作废**：触发锚点之后的主流程记录链以撤销语义作废（含执行重置的当前待办，重置说明写入该记录供审计），主流程退回子流程节点重新等待——**原待办从待办列表消失，新聚合组放行后生成全新的下游待办**；作废链路与 `FlowRevokeService` 一致，逐条推送 `FlowRecordRevokeEvent`。
- **结果判定自愈**：重建实例全部结束后，`FlowSubProcessResultService` 按新组判定（结果脚本经上下文可见继承 + 重建实例的完整最终记录集），放行后生成新的下游待办；`complete()` 定位聚合组时跳过已取代组，避免继承实例同属新旧两组时命中旧组。
- **脚本视野**：`FlowSession.findSubProcessRecords` 过滤已取代组，重置后脚本仅见当前有效组。
- **事件通知**：推送 `FlowSubProcessResetEvent`（旧组快照 + 新组快照 + 重置记录id + 重置操作人 + mock），提醒业务订阅方子流程数据已被重置；订阅方收敛规则见 `docs/Integration/event-integration.md`。

### 可见性（详情标识字段）

重置不是节点动作，不出现在动作列表中。流程详情 `FlowContent` 新增 `resetSubProcess` 布尔标识：当前待办位于「开启重置能力、已汇聚完成」的子流程下游时为 `true`。前端解析到该标识即可自行提供重置交互（呈现方式由使用方决定，不限定于操作列）；节点视图中新旧聚合组同时呈现，`ProcessNode.SubProcessBody.superseded` 与 `SubProcessInstanceBody.inherited/sourceProcessId` 供前端区分。

### 数据结构兼容性

- `SubProcessRecord` 新增 `superseded` 标记、`Instance` 新增 `inherited`/`sourceProcessId` 字段，均为纯增量：持久化实例 JSON 由 fastjson2 按字段读写，旧数据缺失字段按 `false`/`null` 处理；实体新增可空列 `superseded`，存量行为 `null` 时按未取代处理；
- `SubProcessStrategy.resettable`、`FlowContent.resetSubProcess` 均为新增字段，旧数据/旧客户端不受影响；
- 重置不删除任何历史记录，旧聚合组与旧实例记录在其原流程内保持有效可查。

## 运行约束

- 主流程等待期间不允许撤销子流程触发记录。
- 未自动提交的子流程草稿在主流程等待期间不允许删除。
- 结果失败的异常策略仅支持跳转节点，不支持返回兜底操作人。
- 子流程重置仅在节点开启 `resettable` 能力时可用，默认关闭。
- 主流程等待期间（存在等待中聚合组）不允许执行子流程重置。
- 结果未放行（ERROR）的聚合组不允许重置。
- 重置仅允许在锁定合并节点的待办上发起；并行/包容分支的兄弟分支与合并节点之后的更深层节点不可重置。
