package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.HashMap;
import java.util.Map;

public class WorkflowRuntimeRepositoryImpl implements WorkflowRuntimeRepository {

    private final Map<Long, WorkflowRuntime> cache = new HashMap<>();
    private long nextId = 1;

    @Override
    public void save(WorkflowRuntime workflowRuntime) {
        if (workflowRuntime.getId() > 0) {
            cache.put(workflowRuntime.getId(), workflowRuntime);
        } else {
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他记录（WorkflowRuntime id 被 FlowRecord.workRuntimeId 引用）
            workflowRuntime.setId(nextId++);
            cache.put(workflowRuntime.getId(), workflowRuntime);
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
