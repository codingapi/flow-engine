package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.factory.FlowActionFactory;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
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
    @SuppressWarnings("unchecked")
    public static <T extends BaseFlowNode> T loadFromMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setOrder(Integer.parseInt((String) map.get("order")));
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


    @Override
    public void verifyNode(FormMeta form) {

    }

    @Override
    public boolean isContinueTrigger(FlowSession session) {
        return true;
    }

    @Override
    public void verifySession(FlowSession session) {

    }

    @Override
    public boolean isDone(FlowSession session) {
        return true;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {

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
