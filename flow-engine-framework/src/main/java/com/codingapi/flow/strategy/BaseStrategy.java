package com.codingapi.flow.strategy;

import com.codingapi.flow.utils.RandomUtils;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;


public abstract class BaseStrategy implements INodeStrategy {

    @Setter
    private String id;

    public BaseStrategy(String id) {
        this.id = id;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        map.put("id", getId());
        return map;
    }

    @Override
    public String getId() {
        return id;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseStrategy strategy) {
            String id = this.getId();
            return strategy.getId().equals(id);
        }
        return super.equals(obj);
    }

    @SneakyThrows
    public static <T extends BaseStrategy> T  fromMap(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) return null;
        T strategy =  clazz.getDeclaredConstructor().newInstance();
        strategy.setId((String) map.get("id"));
        return strategy;
    }
}
