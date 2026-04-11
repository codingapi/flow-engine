package com.codingapi.flow.strategy.node;

/**
 * 操作人选择方式枚举
 */
public enum OperatorSelectType {

    /**
     * 通过 Groovy 脚本动态加载（默认模式，向后兼容）
     */
    SCRIPT,

    /**
     * 发起人设定：流程创建时由发起人为该节点指定操作人
     */
    INITIATOR_SELECT,

    /**
     * 审批人设定：当前节点审批时，审批人为下游该节点指定操作人
     */
    APPROVER_SELECT
}
