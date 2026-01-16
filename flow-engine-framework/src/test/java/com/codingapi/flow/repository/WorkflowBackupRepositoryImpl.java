package com.codingapi.flow.repository;

import com.codingapi.flow.backup.WorkflowBackup;

import java.util.HashMap;
import java.util.Map;

public class WorkflowBackupRepositoryImpl implements WorkflowBackupRepository {

    private final Map<Long, WorkflowBackup> cache = new HashMap<>();

    @Override
    public void save(WorkflowBackup workflowBackup) {
        if (workflowBackup.getId() > 0) {
            cache.put(workflowBackup.getId(), workflowBackup);
        } else {
            long id = cache.size() + 1;
            workflowBackup.setId(id);
            cache.put(id, workflowBackup);
        }
    }

    @Override
    public WorkflowBackup get(long id) {
        return cache.get(id);
    }

    @Override
    public WorkflowBackup getByWorkId(String workId, long workVersion) {
        return cache.values().stream().filter(backup -> backup.getWorkId().equals(workId) && backup.getWorkVersion() == workVersion).findFirst().orElse(null);
    }

    @Override
    public void delete(WorkflowBackup backup) {
        cache.remove(backup.getId());
    }
}
