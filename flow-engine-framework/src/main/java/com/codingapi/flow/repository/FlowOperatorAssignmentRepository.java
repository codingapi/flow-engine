package com.codingapi.flow.repository;

import java.util.List;

/**
 * 操作人手动分配仓库接口
 */
public interface FlowOperatorAssignmentRepository {

    /**
     * 保存操作人分配（幂等，如已存在则覆盖）
     *
     * @param processId   流程实例唯一标识
     * @param nodeId      节点 ID
     * @param operatorIds 操作人 ID 列表
     */
    void save(String processId, String nodeId, List<Long> operatorIds);

    /**
     * 查询指定节点的已分配操作人 ID 列表
     *
     * @param processId 流程实例唯一标识
     * @param nodeId    节点 ID
     * @return 操作人 ID 列表，不存在时返回空列表
     */
    List<Long> findOperatorIds(String processId, String nodeId);
}
