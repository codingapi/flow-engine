package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.operator.NodeOperators;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public abstract class BaseNode implements IFlowNode {

    public static final String DEFAULT_VIEW = "default";

    /**
     * 节点id
     */
    @Getter
    private String id;
    /**
     * 节点名称
     */
    @Getter
    private String name;
    /**
     * 渲染视图
     */
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
    private List<FormFieldPermission> formFieldPermissions;

    /**
     * 节点操作
     */
    private List<IFlowAction> actions;

    /**
     * 超时到期时间
     */
    private long timeoutTime;
    /**
     * 是否可合并
     */
    private boolean mergeable;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("view", view);
        map.put("name", name);
        map.put("id", id);
        map.put("operatorScript", operatorScript.getScript());
        map.put("nodeTitleScript", nodeTitleScript.getScript());
        map.put("errorTriggerScript", errorTriggerScript.getScript());
        map.put("formFieldPermissions", formFieldPermissions);
        map.put("type", getType());
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        map.put("timeoutTime", String.valueOf(timeoutTime));
        map.put("mergeable", mergeable);
        return map;
    }


    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends BaseNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setView((String) map.get("view"));
        node.setTimeoutTime(Long.parseLong((String) map.get("timeoutTime")));
        node.setMergeable((boolean) map.get("mergeable"));
        node.setOperatorScript((String) map.get("operatorScript"));
        node.setNodeTitleScript((String) map.get("nodeTitleScript"));
        node.setErrorTriggerScript((String) map.get("errorTriggerScript"));
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) map.get("formFieldsPermissions");
        if (permissions != null) {
            List<FormFieldPermission> permissionList = new ArrayList<>();
            for (Map<String, Object> item : permissions) {
                FormFieldPermission permission = new FormFieldPermission();
                permission.setFormCode((String) item.get("formCode"));
                permission.setFieldName((String) item.get("fieldName"));
                permission.setType(PermissionType.valueOf((String) item.get("type")));
                permissionList.add(permission);
            }
            node.setFormFieldPermissions(permissionList);
        }

        List<Map<String, Object>> actions = (List<Map<String, Object>>) map.get("actions");
        if (actions != null) {
            List<IFlowAction> actionList = new ArrayList<>();
            for (Map<String, Object> item : actions) {
                IFlowAction action = FlowActionFactory.getInstance().createAction(item);
                actionList.add(action);
            }
            node.setActions(actionList);
        }
        return node;
    }

    protected void setView(String view) {
        this.view = view;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setId(String id) {
        this.id = id;
    }

    protected void setActions(List<IFlowAction> actions) {
        this.actions = actions;
    }

    protected void setTimeoutTime(long timeoutTime) {
        this.timeoutTime = timeoutTime;
    }

    protected void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }


    /**
     * 设置审批人配置脚本
     *
     * @param operatorScript 审批人配置脚本
     */
    protected void setOperatorScript(String operatorScript) {
        this.operatorScript = new OperatorLoadScript(operatorScript);
    }

    /**
     * 设置节点待办标题脚本
     *
     * @param nodeTitleScript 节点待办标题脚本
     */
    protected void setNodeTitleScript(String nodeTitleScript) {
        this.nodeTitleScript = new NodeTitleScript(nodeTitleScript);
    }

    /**
     * 错误触发脚本
     *
     * @param errorTriggerScript 错误触发脚本
     */
    protected void setErrorTriggerScript(String errorTriggerScript) {
        this.errorTriggerScript = new ErrorTriggerScript(errorTriggerScript);
    }

    /**
     * 设置表单字段权限
     */
    protected void setFormFieldPermissions(List<FormFieldPermission> permissions) {
        this.formFieldPermissions = permissions;
    }

    @Override
    public List<FormFieldPermission> formFieldsPermissions() {
        return formFieldPermissions;
    }

    @Override
    public List<IFlowAction> actions() {
        return actions;
    }

    @Override
    public IFlowAction getActionById(String id) {
        return actions.stream().filter(item -> item.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public NodeOperators operators(FlowSession flowSession) {
        return new NodeOperators(operatorScript.execute(flowSession));
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

    @Override
    public boolean isMergeable() {
        return mergeable;
    }

    @Override
    public long timeoutTime() {
        return timeoutTime;
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
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions can not be null");
        }

        if (operatorScript == null) {
            throw new IllegalArgumentException("operator can not be null");
        }
        if (nodeTitleScript == null) {
            throw new IllegalArgumentException("nodeTitle can not be null");
        }
    }

    private void verifyPermissions(FormMeta form) {
        Map<String, String> fieldTypes = form.getAllFieldTypeMaps();
        for (FormFieldPermission permission : formFieldPermissions) {
            String key = permission.getFormCode() + "." + permission.getFieldName();
            if (!fieldTypes.containsKey(key)) {
                throw new IllegalArgumentException("field " + key + " not found in form " + form.getName());
            }
        }
    }


}
