package com.codingapi.flow.node;

import com.codingapi.flow.session.FlowSession;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseConditionNode extends BaseFlowNode implements IConditionNode{

    public BaseConditionNode(String id, String name) {
        super(id, name);
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("id", id);
        return map;
    }


    @SneakyThrows
    public static <T extends BaseConditionNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        return node;
    }

    @Override
    public boolean match(FlowSession flowSession) {
        return true;
    }
}
