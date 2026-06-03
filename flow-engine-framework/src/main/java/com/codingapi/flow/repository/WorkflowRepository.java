package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.Workflow;

/**
 * 工作流仓库
 */
public interface WorkflowRepository {

    void save(Workflow workflow);

    Workflow getById(String id);

    Workflow getByCode(String code);

    void delete(String id);

}
