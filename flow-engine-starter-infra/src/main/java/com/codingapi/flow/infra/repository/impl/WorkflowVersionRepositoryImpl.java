package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.WorkflowVersionConvertor;
import com.codingapi.flow.infra.entity.WorkflowVersionEntity;
import com.codingapi.flow.infra.jpa.WorkflowVersionEntityRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.workflow.WorkflowVersion;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class WorkflowVersionRepositoryImpl implements WorkflowVersionRepository {

    private final WorkflowVersionEntityRepository workflowVersionEntityRepository;


    @Override
    public WorkflowVersion get(long id) {
        return WorkflowVersionConvertor.convert(workflowVersionEntityRepository.getWorkflowVersionEntityById(id));
    }

    @Override
    public void delete(String workId) {
        workflowVersionEntityRepository.deleteByWorkId(workId);
    }

    @Override
    public void delete(long id) {
        workflowVersionEntityRepository.deleteById(id);
    }

    @Override
    public List<WorkflowVersion> findVersion(String workId) {
        return workflowVersionEntityRepository.findByWorkId(workId)
                .stream()
                .map(WorkflowVersionConvertor::convert)
                .toList();
    }

    @Override
    public void saveAll(List<WorkflowVersion> versionList) {
        for(WorkflowVersion workflowVersion:versionList){
            this.save(workflowVersion);
        }
    }

    @Override
    public void save(WorkflowVersion workflowVersion) {
        WorkflowVersionEntity entity = WorkflowVersionConvertor.convert(workflowVersion);
        workflowVersionEntityRepository.save(entity);
        workflowVersion.setId(entity.getId());
    }
}
