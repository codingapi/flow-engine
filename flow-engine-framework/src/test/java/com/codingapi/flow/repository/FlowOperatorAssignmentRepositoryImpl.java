package com.codingapi.flow.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowOperatorAssignmentRepositoryImpl implements FlowOperatorAssignmentRepository {

    private final Map<String, List<Long>> cache = new HashMap<>();

    private String key(String processId, String nodeId) {
        return processId + ":" + nodeId;
    }

    @Override
    public void save(String processId, String nodeId, List<Long> operatorIds) {
        cache.put(key(processId, nodeId), operatorIds);
    }

    @Override
    public List<Long> findOperatorIds(String processId, String nodeId) {
        return cache.getOrDefault(key(processId, nodeId), Collections.emptyList());
    }
}
