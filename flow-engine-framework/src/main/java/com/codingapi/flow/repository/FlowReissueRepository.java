package com.codingapi.flow.repository;

import com.codingapi.flow.domain.FlowReissueRecord;

import java.util.List;

/**
 * 已完成流程二次发起关系仓库。
 */
public interface FlowReissueRepository {

    /**
     * 保存二次发起关系记录（新增或更新）。
     *
     * @param record 二次发起关系记录
     */
    void save(FlowReissueRecord record);

    /**
     * 按源流程记录id查询二次发起关系，按记录id升序。
     *
     * @param sourceRecordId 源流程记录id
     * @return 二次发起关系记录列表
     */
    List<FlowReissueRecord> findBySourceRecordId(long sourceRecordId);

    /**
     * 按源流程id查询二次发起关系，按记录id升序。
     *
     * @param sourceProcessId 源流程id
     * @return 二次发起关系记录列表
     */
    List<FlowReissueRecord> findBySourceProcessId(String sourceProcessId);

    /**
     * 按新流程id查询二次发起关系，按记录id升序。
     *
     * @param newProcessId 新流程id
     * @return 二次发起关系记录列表
     */
    List<FlowReissueRecord> findByNewProcessId(String newProcessId);
}
