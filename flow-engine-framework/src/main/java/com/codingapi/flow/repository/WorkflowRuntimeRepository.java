package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

/**
 * 运行时流程仓库
 */
public interface WorkflowRuntimeRepository {

    void save(WorkflowRuntime workflowRuntime);

    WorkflowRuntime get(long id);

    WorkflowRuntime getByWorkId(String workId, long workVersion);

    void delete(WorkflowRuntime backup);

}
