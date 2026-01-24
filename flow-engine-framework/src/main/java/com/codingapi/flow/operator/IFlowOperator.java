package com.codingapi.flow.operator;

/**
 * 流程参与用户
 */
public interface IFlowOperator {

    /**
     * 获取用户ID
     *
     * @return ID
     */
    long getUserId();


    /**
     * 获取用户名称
     *
     * @return 名称
     */
    String getName();


    /**
     * 是否流程管理员
     * 流程管理员可以强制干预流程
     */
    boolean isFlowManager();


    /**
     * 委托操作者
     * 当委托操作者不为空时，当前操作者将由委托操作者执行
     */
    IFlowOperator forwardOperator();


    /**
     * 获取委托之后的真正操作者
     *
     * @param operator 操作者
     * @return 真正的操作者
     */
    default IFlowOperator loadRealForwardOperator(IFlowOperator operator) {
        if (operator.forwardOperator() != null) {
            return loadRealForwardOperator(operator.forwardOperator());
        }
        return operator;
    }

}

