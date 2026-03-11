package com.codingapi.flow.mock.repository;

import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRepositoryMockImpl implements WorkflowRepository {

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
    public void delete(String id) {
        cache.remove(id);
    }
}
