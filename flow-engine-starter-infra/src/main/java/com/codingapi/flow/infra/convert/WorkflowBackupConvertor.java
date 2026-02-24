package com.codingapi.flow.infra.convert;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.infra.entity.WorkflowBackupEntity;

public class WorkflowBackupConvertor {

    public static WorkflowBackupEntity convert(WorkflowBackup workflowBackup) {
        if (workflowBackup == null) {
            return null;
        }
        WorkflowBackupEntity entity = new WorkflowBackupEntity();
        if (workflowBackup.getId() > 0) {
            entity.setId(workflowBackup.getId());
        }
        entity.setWorkflow(workflowBackup.getWorkflow());
        entity.setWorkId(workflowBackup.getWorkId());
        entity.setWorkCode(workflowBackup.getWorkCode());
        entity.setWorkVersion(workflowBackup.getWorkVersion());
        entity.setWorkTitle(workflowBackup.getWorkTitle());
        entity.setCreateTime(workflowBackup.getCreateTime());
        return entity;
    }

    public static WorkflowBackup convert(WorkflowBackupEntity entity) {
        if (entity == null) {
            return null;
        }
        return new WorkflowBackup(entity.getId(), entity.getWorkId(), entity.getWorkCode(), entity.getWorkVersion(), entity.getWorkTitle(), entity.getCreateTime(), entity.getWorkflow());
    }
}
