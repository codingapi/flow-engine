package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface WorkflowEntityRepository extends FastRepository<WorkflowEntity,String> {

    WorkflowEntity getWorkflowEntityById(String id);
}
