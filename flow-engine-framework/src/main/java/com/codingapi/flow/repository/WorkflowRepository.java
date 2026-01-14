package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.Workflow;

public interface WorkflowRepository {

    void save(Workflow workflow);

    Workflow get(String id);

    void delete(Workflow workflow);

}
