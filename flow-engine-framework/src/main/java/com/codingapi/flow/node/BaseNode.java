package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public abstract class BaseNode implements IFlowNode{

    /**
     * 节点id
     */
    @Setter
    @Getter
    private String id;
    /**
     * 节点名称
     */
    @Setter
    @Getter
    private String name;
    /**
     * 节点编号
     */
    @Setter
    @Getter
    private String code;
    /**
     * 渲染视图
     */
    @Setter
    @Getter
    private String view;


    @Override
    public List<FormFieldPermission> formFieldsPermissions() {
        return List.of();
    }

    @Override
    public List<IFlowOperator> operators() {
        return List.of();
    }
}
