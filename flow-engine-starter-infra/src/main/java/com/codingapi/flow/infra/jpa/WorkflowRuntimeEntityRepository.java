package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowRuntimeEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface WorkflowRuntimeEntityRepository extends FastRepository<WorkflowRuntimeEntity,Long> {

    WorkflowRuntimeEntity getWorkflowBackupEntityById(long id);


    WorkflowRuntimeEntity getWorkflowBackupEntityByWorkIdAndWorkVersion(String workId, long workVersion);

}
