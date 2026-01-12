package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public abstract class BaseNode implements IFlowNode{

    public static final String DEFAULT_VIEW = "default";

    /**
     * 节点id
     */
    @Getter
    private final String id;
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

    public BaseNode(String name) {
        this(RandomUtils.generateNodeId(),name,RandomUtils.generateNodeCode());
    }

    public BaseNode(String id, String name, String code) {
       this(id, name, code, DEFAULT_VIEW);
    }

    public BaseNode(String id, String name, String code, String view) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.view = view;
    }

    @Override
    public List<FormFieldPermission> formFieldsPermissions() {
        return List.of();
    }

    @Override
    public List<IFlowOperator> operators() {
        return List.of();
    }
}
