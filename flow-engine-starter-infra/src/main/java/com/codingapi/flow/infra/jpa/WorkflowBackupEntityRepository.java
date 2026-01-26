package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowBackupEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface WorkflowBackupEntityRepository extends FastRepository<WorkflowBackupEntity,Long> {

    WorkflowBackupEntity getWorkflowBackupEntityById(long id);


    WorkflowBackupEntity getWorkflowBackupEntityByWorkIdAndWorkVersion(String workId,long workVersion);

}
