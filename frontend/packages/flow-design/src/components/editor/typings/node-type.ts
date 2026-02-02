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
    "TriggerStrategy" |
    // 条件分支策略
    "ConditionBranchStrategy" |
    // 并行分支策略
    "ParallelBranchStrategy"|
    // 包容分支策略
    "InclusiveBranchStrategy";


/**
 * 操作类型
 */
export type ActionType =
    // 保存
    'SAVE'|
    // 通过，流程继续往下流转
    'PASS'|
    // 拒绝，拒绝时需要根据拒绝的配置流程来设置
    'REJECT'|
    // 加签，指定给其他人一块审批，以会签模式来处理
    'ADD_AUDIT'|
    // 委派，委派给其他人员来审批，当人员审批完成以后再流程给自己审批
    'DELEGATE'|
    // 退回，退回时需要设置退回的节点
    'RETURN'|
    // 转办，将流程转移给指定用户来审批，需要配置人员匹配范围
    'TRANSFER'|
    // 自定义，自定义按钮，需要配置脚本
    'CUSTOM';


export const actionOptions = [
    {label: '保存', value: 'SAVE'},
    {label: '通过', value: 'PASS'},
    {label: '拒绝', value: 'REJECT'},
    {label: '加签', value: 'ADD_AUDIT'},
    {label: '委派', value: 'DELEGATE'},
    {label: '退回', value: 'RETURN'},
    {label: '转办', value: 'TRANSFER'},
    {label: '自定义', value: 'CUSTOM'},
]