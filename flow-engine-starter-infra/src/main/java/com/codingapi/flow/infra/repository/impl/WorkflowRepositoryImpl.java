package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.WorkflowConvertor;
import com.codingapi.flow.infra.entity.WorkflowEntity;
import com.codingapi.flow.infra.jpa.WorkflowEntityRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final WorkflowEntityRepository workflowEntityRepository;

    @Override
    public void save(Workflow workflow) {
        WorkflowEntity entity = WorkflowConvertor.convert(workflow);
        workflowEntityRepository.save(entity);
    }

    @Override
    public Workflow get(String id) {
        WorkflowEntity entity = workflowEntityRepository.getWorkflowEntityById(id);
        return WorkflowConvertor.convert(entity);
    }

    @Override
    public void delete(Workflow workflow) {
        workflowEntityRepository.deleteById(workflow.getId());
    }
}
