package com.codingapi.flow.mock.repository;

import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.repository.FlowReissueRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowReissueRepositoryMockImpl implements FlowReissueRepository {

    private final Map<Long, FlowReissueRecord> cache = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized void save(FlowReissueRecord record) {
        if (record.getId() == 0) {
            // 使用单调递增 id，避免与已存在的记录主键冲突
            record.setId(nextId++);
        }
        cache.put(record.getId(), record);
    }

    @Override
    public synchronized List<FlowReissueRecord> findBySourceRecordId(long sourceRecordId) {
        return cache.values().stream()
                .filter(record -> record.getSourceRecordId() == sourceRecordId)
                .toList();
    }

    @Override
    public synchronized List<FlowReissueRecord> findBySourceProcessId(String sourceProcessId) {
        return cache.values().stream()
                .filter(record -> record.getSourceProcessId().equals(sourceProcessId))
                .toList();
    }

    @Override
    public synchronized List<FlowReissueRecord> findByNewProcessId(String newProcessId) {
        return cache.values().stream()
                .filter(record -> record.getNewProcessId().equals(newProcessId))
                .toList();
    }
}
