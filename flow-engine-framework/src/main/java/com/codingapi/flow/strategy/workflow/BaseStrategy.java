package com.codingapi.flow.strategy.workflow;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseStrategy implements IWorkflowStrategy{

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        return map;
    }
}
