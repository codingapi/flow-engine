package com.codingapi.flow.repository;

import com.codingapi.flow.operator.OperatorKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowOperatorAssignmentRepositoryImpl implements FlowOperatorAssignmentRepository {

    private final Map<String, List<OperatorKey>> cache = new HashMap<>();

    private String key(String processId, String nodeId) {
        return processId + ":" + nodeId;
    }

    @Override
    public void save(String processId, String nodeId, List<Long> operatorIds) {
        this.saveOperators(processId, nodeId, operatorIds.stream().map(OperatorKey::ofDefault).toList());
    }

    @Override
    public List<Long> findOperatorIds(String processId, String nodeId) {
        return this.findOperators(processId, nodeId).stream().map(OperatorKey::operatorId).toList();
    }

    @Override
    public void saveOperators(String processId, String nodeId, List<OperatorKey> operators) {
        cache.put(key(processId, nodeId), operators);
    }

    @Override
    public List<OperatorKey> findOperators(String processId, String nodeId) {
        return cache.getOrDefault(key(processId, nodeId), Collections.emptyList());
    }
}
