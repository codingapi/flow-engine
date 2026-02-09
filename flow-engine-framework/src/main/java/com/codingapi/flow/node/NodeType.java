package com.codingapi.flow.node;

/**
 * 节点类型
 */
public enum NodeType {
    // 审批
    APPROVAL,
    // 分支控制
    CONDITION,
    // 分支节点
    CONDITION_BRANCH,
    // 延迟节点
    DELAY,
    // 结束
    END,
    // 办理
    HANDLE,
    // 包容控制
    INCLUSIVE,
    // 包容分支
    INCLUSIVE_BRANCH,
    // 抄送
    NOTIFY,
    // 并行控制
    PARALLEL,
    // 并行分支
    PARALLEL_BRANCH,
    // 路由
    ROUTER,
    // 开始
    START,
    // 子流程
    SUB_PROCESS,
    // 触发
    TRIGGER
}
