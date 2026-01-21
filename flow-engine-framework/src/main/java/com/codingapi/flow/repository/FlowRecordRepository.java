package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

import java.util.List;

public interface FlowRecordRepository {

    FlowRecord get(long id);

    void save(FlowRecord flowRecord);

    void saveAll(List<FlowRecord> flowRecords);

    void delete(FlowRecord flowRecord);

    List<FlowRecord> findRecordsByFromIdAndNodeId(long fromId,String nodeId);

    List<FlowRecord> findRecordsByProcessId(String processId);
}
