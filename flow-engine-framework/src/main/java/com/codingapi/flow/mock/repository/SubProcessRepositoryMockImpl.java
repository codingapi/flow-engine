package com.codingapi.flow.mock.repository;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.repository.SubProcessRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SubProcessRepositoryMockImpl implements SubProcessRepository {

    private final Map<Long, SubProcessRecord> cache = new LinkedHashMap<>();

    @Override
    public synchronized void save(SubProcessRecord record) {
        if (record.getId() == 0) {
            record.setId(cache.size() + 1L);
        }
        cache.put(record.getId(), record);
    }

    @Override
    public synchronized List<SubProcessRecord> findByParentRecordId(long parentRecordId) {
        return cache.values().stream()
                .filter(record -> record.getParentRecordId() == parentRecordId)
                .toList();
    }

    @Override
    public synchronized List<SubProcessRecord> findByParentProcessId(String parentProcessId) {
        return cache.values().stream()
                .filter(record -> record.getParentProcessId().equals(parentProcessId))
                .toList();
    }

    @Override
    public synchronized List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId) {
        return cache.values().stream()
                .filter(record -> record.getParentProcessId().equals(parentProcessId)
                        && record.getNodeId().equals(nodeId))
                .toList();
    }

}
