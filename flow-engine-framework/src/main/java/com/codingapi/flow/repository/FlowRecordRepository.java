package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

public interface FlowRecordRepository {

    FlowRecord get(long id);

    void save(FlowRecord flowRecord);

    void delete(FlowRecord flowRecord);
}
