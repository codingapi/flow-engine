package com.codingapi.flow.operator;

import com.codingapi.springboot.framework.user.IUser;

/**
 * 流程参与用户
 */
public interface IFlowOperator extends IUser {

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
     * 转交审批人
     * 转交审批人不为空时，当前操作者将由转交审批人操作者执行
     */
    IFlowOperator forwardOperator();


}

