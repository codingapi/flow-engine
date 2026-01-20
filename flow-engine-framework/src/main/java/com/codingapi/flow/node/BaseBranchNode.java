package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseBranchNode extends BaseFlowNode implements IFlowNode {

    public BaseBranchNode(String id, String name) {
        super(id, name);
    }

    /**
     * 条件顺序,越小则优先级越高
     */
    @Getter
    @Setter
    private int order;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("id", id);
        map.put("type", getType());
        map.put("order", String.valueOf(order));
        return map;
    }


    @SneakyThrows
    public static <T extends BaseBranchNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = clazz.getDeclaredConstructor().newInstance();
        node.setId((String) map.get("id"));
        node.setName((String) map.get("name"));
        node.setOrder(Integer.parseInt((String) map.get("order")));
        return node;
    }


    public int order() {
        return order;
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

}
