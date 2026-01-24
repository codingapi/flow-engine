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
     * TODO 在流程处理过程中要以抄送的方式给当事人发送信息
     * 当委托操作者不为空时，当前操作者将由委托操作者执行
     */
    IFlowOperator forwardOperator();

}

