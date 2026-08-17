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

## 运行约束

- 主流程等待期间不允许撤销子流程触发记录。
- 未自动提交的子流程草稿在主流程等待期间不允许删除。
- 结果失败的异常策略仅支持跳转节点，不支持返回兜底操作人。
