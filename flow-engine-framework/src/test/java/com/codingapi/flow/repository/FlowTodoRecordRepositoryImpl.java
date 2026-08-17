package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowTodoRecordRepositoryImpl implements FlowTodoRecordRepository {

    private final Map<Long, FlowTodoRecord> cache = new HashMap<>();
    private final Map<String, FlowTodoRecord> cacheByMageKey = new HashMap<>();
    private long nextId = 1;

    // 查询计数器，供测试断言"按 key 逐条查询已被批量查询替代"（N+1 消除）
    private int getByTodoKeyCalls;
    private int findByKeysCalls;

    @Override
    public void save(FlowTodoRecord record) {
        if (record.getId() > 0) {
            cache.put(record.getId(), record);
        } else {
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他待办（多级流程中 B 待办删除后新建 C 待办会冲突）
            record.setId(nextId++);
            cache.put(record.getId(), record);
        }
        cacheByMageKey.put(record.getTodoKey(), record);
    }

    @Override
    public void saveAll(List<FlowTodoRecord> margeRecords) {
        for (FlowTodoRecord record : margeRecords){
            this.save(record);
        }
    }

    @Override
    public void delete(FlowTodoRecord margeRecord) {
        cacheByMageKey.remove(margeRecord.getTodoKey());
        cache.remove(margeRecord.getId());
    }

    @Override
    public FlowTodoRecord getByTodoKey(String key) {
        getByTodoKeyCalls++;
        return cacheByMageKey.get(key);
    }

    @Override
    public List<FlowTodoRecord> findByKeys(List<String> keys) {
        findByKeysCalls++;
        Set<String> keySet = new HashSet<>(keys);
        return cacheByMageKey.entrySet().stream()
                .filter(entry -> keySet.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    public int getGetByTodoKeyCalls() {
        return getByTodoKeyCalls;
    }

    public int getFindByKeysCalls() {
        return findByKeysCalls;
    }

    public List<FlowTodoRecord> findByOperatorId(long operatorId) {
        return cache.values().stream().filter(record -> record.getCurrentOperatorId() == operatorId).toList();
    }

    public List<FlowTodoRecord> findAll() {
        return cache.values().stream().toList();
    }

}
