package com.codingapi.flow.repository;

import com.codingapi.flow.domain.SubProcessRecord;

import java.util.List;

/**
 * 子流程执行记录仓库。
 */
public interface SubProcessRepository {

    /**
     * 保存子流程执行记录（新增或更新）。
     *
     * @param record 子流程执行记录
     */
    void save(SubProcessRecord record);

    /**
     * 按父流程中子流程节点的执行记录id查询该节点产生的全部子流程执行记录，按记录id升序。
     *
     * @param parentRecordId 父流程中子流程节点的执行记录id
     * @return 子流程执行记录列表
     */
    List<SubProcessRecord> findByParentRecordId(long parentRecordId);

    /**
     * 按父流程（主流程）的流程id查询其产生的全部子流程执行记录，按记录id升序。
     *
     * @param parentProcessId 父流程（主流程）的流程id
     * @return 子流程执行记录列表
     */
    List<SubProcessRecord> findByParentProcessId(String parentProcessId);

    /**
     * 按父流程（主流程）的流程id与节点id查询子流程执行记录，按记录id升序。
     *
     * @param parentProcessId 父流程（主流程）的流程id
     * @param nodeId          父流程中子流程节点的节点id
     * @return 子流程执行记录列表
     */
    List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId);
}