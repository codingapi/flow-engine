package com.codingapi.flow.mock.repository;

import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.workflow.WorkflowVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowVersionRepositoryMockImpl implements WorkflowVersionRepository {

    private final Map<Long, WorkflowVersion> cache = new HashMap<>();
    private long nextId = 1;

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
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他版本
            workflowVersion.setId(nextId++);
            cache.put(workflowVersion.getId(), workflowVersion);
        }
    }
}
