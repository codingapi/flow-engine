package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseConfigNode extends BaseFlowNode implements IFlowNode {

    public BaseConfigNode(String id, String name) {
        super(id, name);
    }

    @Override
    public void verifyNode(FormMeta form) {
        this.verifyFields();
    }

    private void verifyFields() {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name can not be null");
        }
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("id can not be null");
        }
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("id", id);
        map.put("type", getType());
        return map;
    }


    @SneakyThrows
    public static <T extends BaseConfigNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        return node;
    }

    @Override
    public ActionManager actions() {
        return new ActionManager(new ArrayList<>());
    }
}
