package com.codingapi.flow.strategy;

import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Map;

public class NodeStrategyFactory {

    @Getter
    private static final NodeStrategyFactory instance = new NodeStrategyFactory();

    private NodeStrategyFactory() {
        this.init();
    }

    private final Map<String, Class<? extends INodeStrategy>> strategyClasses = new java.util.HashMap<>();

    private void init() {
        strategyClasses.put(AdviceStrategy.class.getSimpleName(), AdviceStrategy.class);
        strategyClasses.put(MultiOperatorAuditStrategy.class.getSimpleName(), MultiOperatorAuditStrategy.class);
        strategyClasses.put(RecordMergeStrategy.class.getSimpleName(), RecordMergeStrategy.class);
        strategyClasses.put(ResubmitStrategy.class.getSimpleName(), ResubmitStrategy.class);
        strategyClasses.put(SameOperatorAuditStrategy.class.getSimpleName(), SameOperatorAuditStrategy.class);
        strategyClasses.put(TimeoutStrategy.class.getSimpleName(), TimeoutStrategy.class);
    }


    @SneakyThrows
    public INodeStrategy createStrategy(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        String key = (String) map.get(INodeStrategy.TYPE_KEY);
        Class<? extends INodeStrategy> clazz = strategyClasses.get(key);
        if (clazz != null) {
            Method fromMap = clazz.getMethod("fromMap", Map.class);
            return (INodeStrategy) fromMap.invoke(null, map);
        }
        return null;
    }
}
