package com.codingapi.flow.repository;

import com.codingapi.flow.workflow.Workflow;

/**
 * 工作流仓库
 */
public interface WorkflowRepository {

    void save(Workflow workflow);

    Workflow getById(String id);

    Workflow getByCode(String code);

    /**
     * 锁定指定流程。
     * <p>
     * 持久化仓储应使用数据库行锁实现，以保证依赖流程维度的创建操作在多实例部署下串行执行。
     * 非持久化仓储可以不实现锁。
     *
     * @param id 流程id
     */
    default void lockById(String id) {
    }

    void delete(String id);

}
