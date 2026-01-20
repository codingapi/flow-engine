package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.PassAction;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.builder.BaseNodeBuilder;
import com.codingapi.flow.node.builder.IFormFieldPermissionsNode;
import com.codingapi.flow.node.builder.NodeMapBuilder;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.NodeTitleScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 开始节点
 */
public class StartNode extends BaseFlowNode implements IFormFieldPermissionsNode {

    public static final String NODE_TYPE = "start";
    public static final String DEFAULT_NAME = "开始节点";

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;

    /**
     * 节点待办标题脚本
     */
    private NodeTitleScript nodeTitleScript;

    /**
     * 表单字段权限
     */
    @Setter
    private List<FormFieldPermission> formFieldPermissions;


    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public String generateTitle(FlowSession flowSession) {
        return nodeTitleScript.execute(flowSession);
    }


    public void setNodeTitleScript(String script) {
        this.nodeTitleScript = new NodeTitleScript(script);
    }

    public StartNode(String id, String name, List<IFlowAction> actions, String view, NodeTitleScript nodeTitleScript, List<FormFieldPermission> formFieldPermissions) {
        super(id, name, actions);
        this.view = view;
        this.nodeTitleScript = nodeTitleScript;
        this.formFieldPermissions = formFieldPermissions;
    }

    public StartNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, defaultActions(), DEFAULT_VIEW, NodeTitleScript.defaultScript(), new ArrayList<>());
    }


    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        List<FlowRecord> records = new ArrayList<>();
        FlowRecord currentRecord = session.getCurrentRecord();
        IFlowOperator operator = session.getCurrentOperator();
        IFlowAction action = session.getCurrentAction();
        if (currentRecord == null) {
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), action.id(), RandomUtils.generateStringId(), 0, 0);
            records.add(flowRecord);
        } else {
            // 获取流程创建者
            IFlowOperator creatorOperator = GatewayContext.getInstance().getFlowOperator(currentRecord.getCreateOperatorId());
            FlowRecord flowRecord = new FlowRecord(session.updateSession(creatorOperator), action.id(), currentRecord.getProcessId(), currentRecord.getId(), 0);
            records.add(flowRecord);
        }
        return records;
    }

    @Override
    public FieldPermissionManager formFieldsPermissionsManager() {
        return new FieldPermissionManager(formFieldPermissions);
    }

    private static List<IFlowAction> defaultActions() {
        List<IFlowAction> actions = new ArrayList<>();
        actions.add(new PassAction());
        return actions;
    }

    public static StartNode formMap(Map<String, Object> map) {
        StartNode startNode = BaseFlowNode.loadFromMap(map, StartNode.class);
        startNode.setNodeTitleScript((String) map.get("nodeTitleScript"));
        startNode.setFormFieldPermissions(NodeMapBuilder.loadFormFieldPermissions(map));
        return startNode;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("name", name);
        map.put("id", id);
        map.put("view", view);
        map.put("nodeTitleScript", nodeTitleScript.getScript());
        map.put("formFieldPermissions", formFieldPermissions);
        map.put("type", getType());
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        return map;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        flowRecord.setTitle(this.generateTitle(session));
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, StartNode> {

        public Builder() {
            super(new StartNode());
        }

        public Builder formFieldsPermissions(List<FormFieldPermission> permissions) {
            node.setFormFieldPermissions(permissions);
            return this;
        }
    }
}
