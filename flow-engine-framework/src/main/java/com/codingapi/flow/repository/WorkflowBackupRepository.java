package com.codingapi.flow.repository;

import com.codingapi.flow.backup.WorkflowBackup;

public interface WorkflowBackupRepository {

    void save(WorkflowBackup workflowBackup);

    WorkflowBackup get(long id);

    void delete(WorkflowBackup backup);

}
