package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.WorkflowVersion;

import java.util.List;

/**
 * 工作流版本仓库
 */
public interface WorkflowVersionRepository {

    WorkflowVersion get(long id);

    void delete(String workId);

    List<WorkflowVersion> findVersion(String workId);

    void saveAll(List<WorkflowVersion> versionList);

    void save(WorkflowVersion workflowVersion);

    void delete(long id);

}
