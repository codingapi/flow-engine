package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
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
     * 节点代码
     */
    String getCode();

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
}
