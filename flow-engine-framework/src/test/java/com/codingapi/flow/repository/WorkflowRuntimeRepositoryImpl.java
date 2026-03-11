package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRuntimeRepositoryImpl implements WorkflowRuntimeRepository {

    private final Map<Long, WorkflowRuntime> cache = new HashMap<>();

    @Override
    public void save(WorkflowRuntime workflowRuntime) {
        if (workflowRuntime.getId() > 0) {
            cache.put(workflowRuntime.getId(), workflowRuntime);
        } else {
            long id = cache.size() + 1;
            workflowRuntime.setId(id);
            cache.put(id, workflowRuntime);
        }
    }

    @Override
    public WorkflowRuntime get(long id) {
        return cache.get(id);
    }

    @Override
    public WorkflowRuntime getByWorkId(String workId, long workVersion) {
        return cache.values().stream().filter(backup -> backup.getWorkId().equals(workId) && backup.getWorkVersion() == workVersion).findFirst().orElse(null);
    }

    @Override
    public void delete(WorkflowRuntime backup) {
        cache.remove(backup.getId());
    }
}
