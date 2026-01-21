package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
public abstract class BaseAction implements IFlowAction {

    protected String id;
    protected ActionType type;
    protected String title;
    protected ActionDisplay display;

    @Override
    public ActionType type() {
        return type;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public ActionDisplay display() {
        return display;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BaseAction action){
            return action.getId().equals(id);
        }
        return super.equals(obj);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("title", title);
        map.put("display", display != null ? display.toMap() : null);
        return map;
    }

    @Override
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
        return List.of();
    }

    @Override
    public void copy(IFlowAction action) {
        this.id = action.id();
        this.type = action.type();
        this.title = action.title();
        this.display = action.display();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected static <T extends BaseAction> T fromMap(Map<String, Object> data, Class<T> clazz) {
        T action = clazz.getDeclaredConstructor().newInstance();
        action.setId((String) data.get("id"));
        action.setType(ActionType.valueOf((String) data.get("type")));
        action.setTitle((String) data.get("title"));
        Map<String, Object> display = (Map<String, Object>) data.get("display");
        if (display != null) {
            action.setDisplay(ActionDisplay.fromMap(display));
        }
        return action;
    }


    @Override
    public void run(FlowSession flowSession) {}

    /**
     * 触发并执行后续节点
     * @param flowSession 当前会话
     * @param consumer 节点处理
     */
    public void triggerNode(FlowSession flowSession, Consumer<FlowSession> consumer) {
        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        for (IFlowNode node : nextNodes) {
            FlowSession triggerSession = flowSession.updateSession(node);
            if (node.continueTrigger(triggerSession)) {
                this.triggerNode(triggerSession,consumer);
            }else {
                if (consumer != null) {
                    consumer.accept(triggerSession);
                }
            }
        }
    }
}
