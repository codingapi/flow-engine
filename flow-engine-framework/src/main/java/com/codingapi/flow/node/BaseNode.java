package com.codingapi.flow.node;

import com.codingapi.flow.action.*;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.script.node.ErrorTriggerScript;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.NodeStrategyFactory;
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
     * 节点策略
     */
    private List<INodeStrategy> nodeStrategies;

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
        map.put("nodeStrategies", nodeStrategies.stream().map(INodeStrategy::toMap).toList());
        return map;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T extends BaseNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setView((String) map.get("view"));
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

        List<Map<String, Object>> nodeStrategies = (List<Map<String, Object>>) map.get("nodeStrategies");
        if (nodeStrategies != null) {
            List<INodeStrategy> strategyList = new ArrayList<>();
            for (Map<String, Object> item : nodeStrategies) {
                INodeStrategy strategy = NodeStrategyFactory.getInstance().createStrategy(item);
                strategyList.add(strategy);
            }
            node.setNodeStrategies(strategyList);
        }

        return node;
    }

    protected void setNodeStrategies(List<INodeStrategy> strategyList) {
        this.nodeStrategies = strategyList;
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
    public FieldPermissionManager formFieldsPermissions() {
        return new FieldPermissionManager(formFieldPermissions);
    }

    @Override
    public ActionManager actions() {
        return new ActionManager(actions);
    }


    @Override
    public OperatorManager operators(FlowSession flowSession) {
        return new OperatorManager(operatorScript.execute(flowSession));
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
    public void addAction(IFlowAction action) {
        if(this.actions == null){
            this.actions = new ArrayList<>();
        }
        this.actions.add(action);
    }

    @Override
    public void verify(FormMeta form) {
        this.verifyFields();
        if (!(this instanceof EndNode)) {
            FieldPermissionManager fieldPermissionManager = this.formFieldsPermissions();
            fieldPermissionManager.verifyPermissions(form);
        }
    }

    @Override
    public StrategyManager strategies() {
        return new StrategyManager(nodeStrategies);
    }

    @Override
    public boolean match(FlowSession flowSession) {
        return true;
    }

    @Override
    public void verifyFlowAdvice(FlowAdvice flowAdvice) {
        StrategyManager strategyManager = this.strategies();
        IFlowAction flowAction = flowAdvice.getAction();

        // 保存操作,不做检查
        if(flowAction instanceof SaveAction){
            return;
        }

        // 转办操作
        if(flowAction instanceof TransferAction){
            if(flowAdvice.getTransferOperators()==null || flowAdvice.getTransferOperators().isEmpty()){
                throw new IllegalArgumentException("transferOperators can not be null");
            }
        }

        // 退回操作
        if(flowAction instanceof ReturnAction){
            if(flowAdvice.getBackNode()==null ){
                throw new IllegalArgumentException("backNode can not be null");
            }
        }

        // 是否必须填写审批意见
        if(strategyManager.isEnableAdvice()){
            if(!StringUtils.hasText(flowAdvice.getAdvice())){
                throw new IllegalArgumentException("advice can not be null");
            }
        }
        //  通过操作
        if(flowAction instanceof PassAction) {
            // 是否必须签名
            if (strategyManager.isEnableSignable()) {
                if (!StringUtils.hasText(flowAdvice.getSignKey())) {
                    throw new IllegalArgumentException("signKey can not be null");
                }
            }
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


}
