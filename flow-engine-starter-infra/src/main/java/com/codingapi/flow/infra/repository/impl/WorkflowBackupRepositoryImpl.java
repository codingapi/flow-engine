package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.infra.convert.WorkflowBackupConvertor;
import com.codingapi.flow.infra.entity.WorkflowBackupEntity;
import com.codingapi.flow.infra.jpa.WorkflowBackupEntityRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WorkflowBackupRepositoryImpl implements WorkflowBackupRepository {

    private final WorkflowBackupEntityRepository workflowBackupEntityRepository;

    @Override
    public void save(WorkflowBackup workflowBackup) {
        WorkflowBackupEntity entity = WorkflowBackupConvertor.convert(workflowBackup);
        workflowBackupEntityRepository.save(entity);
        workflowBackup.setId(entity.getId());
    }

    @Override
    public WorkflowBackup get(long id) {
        WorkflowBackupEntity entity = workflowBackupEntityRepository.getWorkflowBackupEntityById(id);
        return WorkflowBackupConvertor.convert(entity);
    }

    @Override
    public WorkflowBackup getByWorkId(String workId, long workVersion) {
        WorkflowBackupEntity entity = workflowBackupEntityRepository.getWorkflowBackupEntityByWorkIdAndWorkVersion(workId, workVersion);
        return WorkflowBackupConvertor.convert(entity);
    }

    @Override
    public void delete(WorkflowBackup backup) {
        workflowBackupEntityRepository.deleteById(backup.getId());
    }
}
