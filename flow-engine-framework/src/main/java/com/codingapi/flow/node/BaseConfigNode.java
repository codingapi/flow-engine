package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.session.FlowSession;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseConfigNode extends BaseFlowNode implements IConfigNode {

    public BaseConfigNode(String id, String name) {
        super(id, name);
    }

    @Override
    public void verify(FormMeta form) {
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
    public void execute(FlowSession flowSession) {

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

}
