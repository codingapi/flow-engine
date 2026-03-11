package com.codingapi.flow.mock.repository;

import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTodoRecordRepositoryMockImpl implements FlowTodoRecordRepository {

    private final Map<Long, FlowTodoRecord> cache = new HashMap<>();
    private final Map<String, FlowTodoRecord> cacheByMageKey = new HashMap<>();

    @Override
    public void save(FlowTodoRecord record) {
        if (record.getId() > 0) {
            cache.put(record.getId(), record);
        } else {
            long id = cache.size() + 1;
            record.setId(id);
            cache.put(id, record);
        }
        cacheByMageKey.put(record.getMergeKey(), record);
    }

    @Override
    public void saveAll(List<FlowTodoRecord> margeRecords) {
        for (FlowTodoRecord record : margeRecords){
            this.save(record);
        }
    }

    @Override
    public void delete(FlowTodoRecord margeRecord) {
        cacheByMageKey.remove(margeRecord.getMergeKey());
        cache.remove(margeRecord.getId());
    }

    @Override
    public FlowTodoRecord getByTodoKey(String key) {
        return cacheByMageKey.get(key);
    }

    public List<FlowTodoRecord> findByOperatorId(long operatorId) {
        return cache.values().stream().filter(record -> record.getCurrentOperatorId() == operatorId).toList();
    }

    public List<FlowTodoRecord> findAll() {
        return cache.values().stream().toList();
    }

}
