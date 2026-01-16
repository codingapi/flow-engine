package com.codingapi.flow.node;

import com.codingapi.flow.action.FlowAction;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.operator.NodeOperators;
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
    List<FlowAction> actions();

    /**
     * 获取节点动作
     * @param id 动作id
     */
    FlowAction getActionById(String id);

    /**
     * 获取节点动作
     * @param title 动作title
     */
    FlowAction getActionByTitle(String title);

    /**
     * 表单字段权限设置
     */
    List<FormFieldPermission> formFieldsPermissions();

    /**
     * 节点参与用户
     */
    NodeOperators operators(FlowSession flowSession);

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
    Map<String,Object> toMap();

    /**
     * 超时到期时间(毫秒)
     */
    long timeoutTime();
    /**
     * 是否可合并
     */
    boolean isMergeable();


}
