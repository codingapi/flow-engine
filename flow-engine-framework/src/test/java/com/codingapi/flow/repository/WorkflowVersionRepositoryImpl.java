package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.WorkflowVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository{

    private final Map<Long, WorkflowVersion> cache = new HashMap<>();

    @Override
    public WorkflowVersion get(long id) {
        return cache.get(id);
    }

    @Override
    public void delete(String workId) {
        List<Long> keys = new ArrayList<>();
        for (WorkflowVersion version:cache.values()){
            if(version.getWorkId().equals(workId)){
                keys.add(version.getId());
            }
        }
        for (long key:keys){
            this.cache.remove(key);
        }
    }


    @Override
    public void delete(long id) {
        this.cache.remove(id);
    }

    @Override
    public List<WorkflowVersion> findVersion(String workId) {
        return cache.values().stream()
                .filter(workflowVersion -> workflowVersion.getWorkId().equals(workId))
                .toList();
    }


    @Override
    public void saveAll(List<WorkflowVersion> versionList) {
        for (WorkflowVersion version:versionList){
            this.save(version);
        }
    }

    @Override
    public void save(WorkflowVersion workflowVersion) {
        if (workflowVersion.getId() > 0) {
            cache.put(workflowVersion.getId(), workflowVersion);
        } else {
            long id = cache.size() + 1;
            workflowVersion.setId(id);
            cache.put(id, workflowVersion);
        }
    }
}
