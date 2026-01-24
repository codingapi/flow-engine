package com.codingapi.flow.strategy.workflow;

import com.codingapi.flow.strategy.node.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Map;

public class WorkflowStrategyFactory {


    @Getter
    private static final WorkflowStrategyFactory instance = new WorkflowStrategyFactory();

    private WorkflowStrategyFactory() {
        this.init();
    }

    private final Map<String, Class<? extends IWorkflowStrategy>> strategyClasses = new java.util.HashMap<>();

    private void init() {
        strategyClasses.put(InterfereStrategy.class.getSimpleName(), InterfereStrategy.class);
        strategyClasses.put(UrgeStrategy.class.getSimpleName(), UrgeStrategy.class);
    }


    @SneakyThrows
    public IWorkflowStrategy createStrategy(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        String key = (String) map.get(IWorkflowStrategy.TYPE_KEY);
        Class<? extends IWorkflowStrategy> clazz = strategyClasses.get(key);
        if (clazz != null) {
            Method fromMap = clazz.getMethod("fromMap", Map.class);
            return (IWorkflowStrategy) fromMap.invoke(null, map);
        }
        return null;
    }
}
