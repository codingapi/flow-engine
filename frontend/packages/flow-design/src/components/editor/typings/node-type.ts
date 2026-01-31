/**
 * 节点类型
 */
export type NodeType =
    // 审批
    "APPROVAL" |
    // 分支控制
    "CONDITION" |
    // 分支节点
    "CONDITION_BRANCH" |
    // 延迟节点
    "DELAY" |
    // 结束
    "END" |
    // 办理
    "HANDLE" |
    // 包容控制
    "INCLUSIVE" |
    // 包容分支
    "INCLUSIVE_BRANCH" |
    // 抄送
    "NOTIFY" |
    // 并行控制
    "PARALLEL" |
    // 并行分支
    "PARALLEL_BRANCH" |
    // 路由
    "ROUTER" |
    // 开始
    "START" |
    // 子流程
    "SUB_PROCESS" |
    // 触发
    "TRIGGER";


export type NodeStrategyType =
    // 节点审批意见策略
    "AdviceStrategy" |
    // 延迟策略配置
    "DelayStrategy" |
    // 错误触发策略配置(没有匹配到人时)
    "ErrorTriggerStrategy" |
    // 表单字段权限策略配置
    "FormFieldPermissionStrategy"|
    // 多人审批策略配置
    "MultiOperatorAuditStrategy"|
    // 节点标题策略配置
    "NodeTitleStrategy"|
    // 操作人配置策略
    "OperatorLoadStrategy"|
    // 记录合并策略配置
    "RecordMergeStrategy"|
    // 重新提交策略配置
    "ResubmitStrategy"|
    // 路由策略配置
    "RouterStrategy"|
    // 撤回策略
    "RevokeStrategy"|
    // 提交人与审批人一致配置
    "SameOperatorAuditStrategy"|
    // 子流程任务策略
    "SubProcessStrategy"|
    // 超时策略配置
    "TimeoutStrategy"|
    // 触发策略配置
    "TriggerStrategy";

