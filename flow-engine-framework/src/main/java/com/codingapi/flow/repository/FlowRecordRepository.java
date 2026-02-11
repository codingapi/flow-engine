package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

import java.util.List;

/**
 * 流程记录
 */
public interface FlowRecordRepository {

    /**
     * 获取流程详细
     * @param id 流程id
     * @return 流程记录
     */
    FlowRecord get(long id);

    /**
     * 保存流程
     * @param flowRecord 流程记录
     */
    void save(FlowRecord flowRecord);

    /**
     * 批量保存流程
     * @param flowRecords 流程记录
     */
    void saveAll(List<FlowRecord> flowRecords);

    /**
     * 删除流程记录
     * @param flowRecord 流程记录
     */
    void delete(FlowRecord flowRecord);

    /**
     * 查询当前节点的记录
     *
     * @param fromId 流程的来源记录id
     * @param nodeId 节点id
     * @return 记录列表
     */
    List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId);

    /**
     * 查询当前流程的记录
     *
     * @param processId 流程id
     * @return 记录列表
     */
    List<FlowRecord> findProcessRecords(String processId);

    /**
     * 查询所有最新的待办记录
     *
     * @param processId 流程id
     * @return 待办记录列表
     */
    List<FlowRecord> findTodoRecords(String processId);

    /**
     * 查询所有后续的流程记录
     *
     * @param processId 流程id
     * @param fromId    开始记录id
     * @return 记录列表
     */
    List<FlowRecord> findAfterRecords(String processId, long fromId);

}
