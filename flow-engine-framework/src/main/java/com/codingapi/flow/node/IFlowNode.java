package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.Map;

/**
 * 流程节点
 */
public interface IFlowNode {

    /**
     * 节点id
     */
    String getId();

    /**
     * 节点名称
     */
    String getName();

    /**
     * 节点视图
     */
    String getView();

    /**
     * 流程类型
     */
    String getType();

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
     * 节点验证
     */
    void verify(FormMeta form);


    /**
     * 转为map
     */
    Map<String, Object> toMap();


    /**
     * 节点策略
     */
    StrategyManager strategies();


    /**
     * 节点是否完成
     *
     * @param session        会话对象
     * @param action         节点动作
     * @param currentRecords 当前节点的记录
     * @return true:完成 false:未完成
     */
    boolean isDone(FlowSession session, IFlowAction action, List<FlowRecord> currentRecords);

}
