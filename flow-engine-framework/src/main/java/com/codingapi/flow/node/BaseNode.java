package com.codingapi.flow.node;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.error.ErrorThrow;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseNode implements IFlowNode {

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
     * 渲染视图
     */
    @Setter
    @Getter
    private String view;

    private final List<FormFieldPermission> formFieldsPermissions;

    public BaseNode(String name) {
        this(RandomUtils.generateStringId(), name, DEFAULT_VIEW);
    }

    public BaseNode(String id, String name) {
        this(id, name, DEFAULT_VIEW);
    }

    public BaseNode(String id, String name, String view) {
        this.id = id;
        this.name = name;
        this.view = view;
        this.formFieldsPermissions = new ArrayList<>();
    }

    @Override
    public List<FormFieldPermission> formFieldsPermissions() {
        return formFieldsPermissions;
    }

    @Override
    public List<IFlowOperator> operators() {
        return List.of();
    }

    public void setFormFieldsPermissions(FormFieldPermission.Builder builder) {
        formFieldsPermissions.addAll(builder.build());
    }

    @Override
    public String generateTitle(FlowSession flowSession) {
        return "";
    }

    @Override
    public ErrorThrow errorTrigger(FlowSession flowSession) {
        return null;
    }

    @Override
    public void verify(FlowForm form) {
        this.verifyFields();
        if (!(this instanceof EndNode)) {
            this.verifyPermissions(form);
        }
    }

    private void verifyFields() {
        if (!StringUtils.hasText(view)) {
            throw new IllegalArgumentException("view can not be null");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name can not be null");
        }
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id can not be null");
        }
    }

    private void verifyPermissions(FlowForm form) {
        Map<String, String> fieldTypes = form.getFieldTypeMaps();
        for (FormFieldPermission permission : formFieldsPermissions) {
            String key = permission.getFormCode() + "." + permission.getFieldName();
            if (!fieldTypes.containsKey(key)) {
                throw new IllegalArgumentException("field " + key + " not found in form " + form.getName());
            }
        }
    }
}
