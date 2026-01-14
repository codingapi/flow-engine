package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
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

    /**
     * 审批人配置脚本
     */
    private OperatorLoadScript operatorScript;

    /**
     * 节点待办标题脚本
     */
    private NodeTitleScript nodeTitleScript;

    /**
     * 异常触发脚本
     */
    private ErrorTriggerScript errorTriggerScript;

    /**
     * 表单字段权限
     */
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
        this.operatorScript = OperatorLoadScript.creator();
        this.nodeTitleScript = NodeTitleScript.defaultScript();
    }

    /**
     * 设置审批人配置脚本
     * @param operatorScript 审批人配置脚本
     */
    public void setOperatorScript(String operatorScript) {
        this.operatorScript = new OperatorLoadScript(operatorScript);
    }

    /**
     * 设置节点待办标题脚本
     * @param nodeTitleScript 节点待办标题脚本
     */
    public void setNodeTitleScript(String nodeTitleScript) {
        this.nodeTitleScript = new NodeTitleScript(nodeTitleScript);
    }

    /**
     * 错误触发脚本
     * @param errorTriggerScript 错误触发脚本
     */
    public void setErrorTriggerScript(String errorTriggerScript) {
        this.errorTriggerScript = new ErrorTriggerScript(errorTriggerScript);
    }

    /**
     * 设置表单字段权限
     * @param builder 表单字段权限构建器
     */
    public void setFormFieldsPermissions(FormFieldPermission.Builder builder) {
        formFieldsPermissions.addAll(builder.build());
    }


    @Override
    public List<FormFieldPermission> formFieldsPermissions() {
        return formFieldsPermissions;
    }

    @Override
    public List<IFlowOperator> operators(FlowSession flowSession) {
        return operatorScript.execute(flowSession);
    }

    @Override
    public String generateTitle(FlowSession flowSession) {
        return nodeTitleScript.execute(flowSession);
    }

    @Override
    public ErrorThrow errorTrigger(FlowSession flowSession) {
        return errorTriggerScript.execute(flowSession);
    }

    @Override
    public void verify(FormMeta form) {
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

        if(operatorScript==null){
            throw new IllegalArgumentException("operator can not be null");
        }
        if(nodeTitleScript==null){
            throw new IllegalArgumentException("nodeTitle can not be null");
        }
    }

    private void verifyPermissions(FormMeta form) {
        Map<String, String> fieldTypes = form.getAllFieldTypeMaps();
        for (FormFieldPermission permission : formFieldsPermissions) {
            String key = permission.getFormCode() + "." + permission.getFieldName();
            if (!fieldTypes.containsKey(key)) {
                throw new IllegalArgumentException("field " + key + " not found in form " + form.getName());
            }
        }
    }
}
