package com.codingapi.flow.node;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.error.ErrorThrow;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.IFlowOperator;

import java.util.List;

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
     * 表单字段权限设置
     */
    List<FormFieldPermission> formFieldsPermissions();

    /**
     * 节点参与用户
     */
    List<IFlowOperator> operators();

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
    void verify(FlowForm form);

}
