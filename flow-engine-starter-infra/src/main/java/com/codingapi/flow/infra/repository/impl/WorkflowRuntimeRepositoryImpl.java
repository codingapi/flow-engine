package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.flow.infra.convert.WorkflowRuntimeConvertor;
import com.codingapi.flow.infra.entity.WorkflowRuntimeEntity;
import com.codingapi.flow.infra.jpa.WorkflowRuntimeEntityRepository;
import com.codingapi.flow.repository.WorkflowRuntimeRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WorkflowRuntimeRepositoryImpl implements WorkflowRuntimeRepository {

    private final WorkflowRuntimeEntityRepository workflowRuntimeEntityRepository;

    @Override
    public void save(WorkflowRuntime workflowRuntime) {
        WorkflowRuntimeEntity entity = WorkflowRuntimeConvertor.convert(workflowRuntime);
        workflowRuntimeEntityRepository.save(entity);
        workflowRuntime.setId(entity.getId());
    }

    @Override
    public WorkflowRuntime get(long id) {
        WorkflowRuntimeEntity entity = workflowRuntimeEntityRepository.getWorkflowBackupEntityById(id);
        return WorkflowRuntimeConvertor.convert(entity);
    }

    @Override
    public WorkflowRuntime getByWorkId(String workId, long workVersion) {
        WorkflowRuntimeEntity entity = workflowRuntimeEntityRepository.getWorkflowBackupEntityByWorkIdAndWorkVersion(workId, workVersion);
        return WorkflowRuntimeConvertor.convert(entity);
    }

    @Override
    public void delete(WorkflowRuntime backup) {
        workflowRuntimeEntityRepository.deleteById(backup.getId());
    }
}
