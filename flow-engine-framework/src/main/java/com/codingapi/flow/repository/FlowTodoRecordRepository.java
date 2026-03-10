package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoRecord;

import java.util.List;

public interface FlowTodoRecordRepository {

    void saveAll(List<FlowTodoRecord> margeRecords);

    FlowTodoRecord getByMergeKey(String key);

    void delete(FlowTodoRecord margeRecord);

    void save(FlowTodoRecord margeRecord);

}
