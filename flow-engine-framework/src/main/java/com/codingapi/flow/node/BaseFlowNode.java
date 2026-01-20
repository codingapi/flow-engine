package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.ReturnAction;
import com.codingapi.flow.action.SaveAction;
import com.codingapi.flow.action.TransferAction;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.builder.NodeMapBuilder;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.node.manager.FieldPermissionManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseFlowNode implements IFlowNode {

    /**
     * 节点id
     */
    @Getter
    @Setter
    protected String id;
    /**
     * 节点名称
     */
    @Getter
    @Setter
    protected String name;

    /**
     * 条件顺序,越小则优先级越高
     */
    @Getter
    @Setter
    protected int order;

    /**
     * 节点操作
     */
    @Setter
    @Getter
    protected List<IFlowAction> actions;

    public BaseFlowNode(String name, String id) {
        this(name, id, 0, new ArrayList<>());
    }

    public BaseFlowNode(String id, String name, int order) {
        this(id, name, order, new ArrayList<>());
    }

    public BaseFlowNode(String id, String name,List<IFlowAction> actions) {
        this(id, name, 0, actions);
    }

    public BaseFlowNode(String id, String name, int order, List<IFlowAction> actions) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.actions = actions;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("type", getType());
        map.put("order", String.valueOf(order));
        map.put("actions", actions.stream().map(IFlowAction::toMap).toList());
        return map;
    }


    @SneakyThrows
    public static <T extends BaseFlowNode> T loadFromMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setOrder(Integer.parseInt((String) map.get("order")));
        node.setActions(NodeMapBuilder.loadActions(map));
        return node;
    }


    @Override
    public void verifyNode(FormMeta form) {

    }

    @Override
    public boolean isContinueTrigger(FlowSession session) {
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {
        FlowAdvice flowAdvice = session.getAdvice();

        IFlowAction flowAction = flowAdvice.getAction();
        // 保存操作,不做检查
        if (flowAction instanceof SaveAction) {
            return;
        }
        // 转办操作
        if (flowAction instanceof TransferAction) {
            if (flowAdvice.getTransferOperators() == null || flowAdvice.getTransferOperators().isEmpty()) {
                throw new IllegalArgumentException("transferOperators can not be null");
            }
        }
        // 退回操作
        if (flowAction instanceof ReturnAction) {
            if (flowAdvice.getBackNode() == null) {
                throw new IllegalArgumentException("backNode can not be null");
            }
        }
    }

    @Override
    public boolean isDone(FlowSession session) {
        return true;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {

    }

    @Override
    public List<IFlowNode> matchBranch(List<IFlowNode> nodeList, FlowSession flowSession) {
        return nodeList;
    }

    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        return List.of();
    }

    @Override
    public ActionManager actions() {
        return new ActionManager(actions);
    }

}
