package com.codingapi.flow.action;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
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


    protected void setId(String id) {
        this.id = id;
    }

    protected void setType(ActionType type) {
        this.type = type;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setDisplay(ActionDisplay display) {
        this.display = display;
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
    public List<FlowRecord> trigger(FlowSession flowSession,FlowRecord currentRecord) {
        return null;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected static <T extends BaseAction> T formMap(Map<String, Object> data, Class<T> clazz) {
        T action = clazz.getDeclaredConstructor().newInstance();
        action.setId((String) data.get("id"));
        action.setType(ActionType.valueOf((String) data.get("type")));
        action.setTitle((String) data.get("title"));
        Map<String,Object> display = (Map<String, Object>) data.get("display");
        if(display!=null) {
            action.setDisplay(ActionDisplay.fromMap(display));
        }
        return action;
    }
}
