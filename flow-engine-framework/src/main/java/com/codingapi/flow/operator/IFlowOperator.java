package com.codingapi.flow.operator;

import com.codingapi.flow.script.request.GroovyScriptRequest;
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
     *
     * @param request 流程会话上下文，可用于根据表单数据、当前节点等条件动态决定转交人
     * @return 转交后的审批人，如果无需转交返回null
     */
    IFlowOperator forwardOperator(GroovyScriptRequest request);


}

