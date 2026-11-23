package com.codingapi.flow.repository;

import com.codingapi.flow.operator.OperatorKey;

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

    /**
     * 保存操作人分配，携带身份标识（issue #288 衍生 / #292）。
     *
     * <p>{@link OperatorKey} 与 {@code List<Long>} 泛型擦除后签名相同，无法重载，因此单独命名。
     * 默认实现忽略身份、退化为 {@link #save(String, String, List)}，存量实现无需改动即可编译，
     * 但会丢失身份；需要支持多身份时覆盖此方法。
     *
     * @param processId 流程实例唯一标识
     * @param nodeId    节点 ID
     * @param operators 操作人列表（含身份）
     */
    default void saveOperators(String processId, String nodeId, List<OperatorKey> operators) {
        save(processId, nodeId, operators.stream().map(OperatorKey::operatorId).toList());
    }

    /**
     * 查询指定节点的已分配操作人列表（含身份，issue #288 衍生 / #292）。
     *
     * <p>默认实现退化为 {@link #findOperatorIds(String, String)}，返回的操作人身份均为默认身份。
     *
     * @param processId 流程实例唯一标识
     * @param nodeId    节点 ID
     * @return 操作人列表，不存在时返回空列表
     */
    default List<OperatorKey> findOperators(String processId, String nodeId) {
        return findOperatorIds(processId, nodeId).stream().map(OperatorKey::ofDefault).toList();
    }
}
