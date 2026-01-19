package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;

import java.util.Map;

/**
 *  审批节点
 */
public interface IAuditNode extends IFlowNode {

    /**
     * 节点视图
     */
    String getView();

    /**
     * 添加节点动作
     */
    void addAction(IFlowAction action);

    /**
     * 节点动作
     */
    ActionManager actions();

    /**
     * 表单字段权限设置
     */
    FieldPermissionManager formFieldsPermissions();

    /**
     * 节点参与用户
     */
    OperatorManager operators(FlowSession flowSession);

    /**
     * 构建待办标题
     */
    String generateTitle(FlowSession flowSession);


    /**
     * 错误异常处理
     */
    ErrorThrow errorTrigger(FlowSession flowSession);

    /**
     * 转为map
     */
    Map<String, Object> toMap();


    /**
     * 节点策略
     */
    StrategyManager strategies();

    /**
     * 校验提交参数
     * @param flowAdvice 请求参数
     */
    void verifyFlowAdvice(FlowAdvice flowAdvice);

}
