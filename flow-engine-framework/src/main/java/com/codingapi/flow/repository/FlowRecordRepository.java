package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

import java.util.List;

public interface FlowRecordRepository {

    FlowRecord get(long id);

    void save(FlowRecord flowRecord);

    void saveAll(List<FlowRecord> flowRecords);

    void delete(FlowRecord flowRecord);

    /**
     * 查询当前节点的记录
     * @param fromId 流程的来源记录id
     * @param nodeId 节点id
     * @return 记录列表
     */
    List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId);

    /**
     * 查询当前流程的记录
     * @param processId 流程id
     * @return 记录列表
     */
    List<FlowRecord> findProcessRecords(String processId);

    /**
     * 查询所有后续的流程记录
     * @param processId 流程id
     * @param fromId 开始记录id
     * @return 记录列表
     */
    List<FlowRecord> findAfterRecords(String processId, long fromId);

}
