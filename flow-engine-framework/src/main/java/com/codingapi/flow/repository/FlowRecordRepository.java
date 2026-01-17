package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

import java.util.List;

public interface FlowRecordRepository {

    FlowRecord get(long id);

    void save(FlowRecord flowRecord);

    void saveAll(List<FlowRecord> flowRecords);

    void delete(FlowRecord flowRecord);
}
