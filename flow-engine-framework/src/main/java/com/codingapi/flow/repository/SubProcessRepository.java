package com.codingapi.flow.repository;

import com.codingapi.flow.domain.SubProcessRecord;

import java.util.List;

/**
 * 子流程执行记录仓库。
 */
public interface SubProcessRepository {

    void save(SubProcessRecord record);

    List<SubProcessRecord> findByParentRecordId(long parentRecordId);

    List<SubProcessRecord> findByParentProcessId(String parentProcessId);

    List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId);
}
