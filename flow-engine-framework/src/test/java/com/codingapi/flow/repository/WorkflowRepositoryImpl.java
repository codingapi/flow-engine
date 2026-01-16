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
    public Workflow get(String id) {
        return cache.get(id);
    }

    @Override
    public void delete(Workflow workflow) {
        cache.remove(workflow.getId());
    }
}
