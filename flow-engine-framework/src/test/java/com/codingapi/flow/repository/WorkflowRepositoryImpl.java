package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.Workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final Map<String, Workflow> cache = new HashMap<>();

    @Override
    public void save(Workflow workflow) {
        cache.put(workflow.getId(), workflow);
    }

    @Override
    public Workflow getById(String id) {
        return cache.get(id);
    }

    @Override
    public Workflow getByCode(String code) {
        for (Workflow workflow:cache.values()) {
            if(workflow.getCode().equals(code)) {
                return workflow;
            }
        }
        return null;
    }

    @Override
    public void delete(String id) {
        cache.remove(id);
    }
}
