package com.codingapi.flow.mock.repository;

import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.repository.FlowTodoMergeRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTodoMergeRepositoryMockImpl implements FlowTodoMergeRepository {

    private final Map<Long, FlowTodoMerge> cache = new HashMap<>();
    private long nextId = 1;

    private void save(FlowTodoMerge relation) {
        if (relation.getId() > 0) {
            cache.put(relation.getId(), relation);
        } else {
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他记录
            relation.setId(nextId++);
            cache.put(relation.getId(), relation);
        }
    }

    @Override
    public void delete(FlowTodoMerge todoMerge) {
        this.cache.remove(todoMerge.getId());
    }

    @Override
    public void saveAll(List<FlowTodoMerge> list) {
        for (FlowTodoMerge relation : list){
            this.save(relation);
        }
    }

    @Override
    public List<FlowTodoMerge> findByTodoId(long todoId) {
        return cache.values().stream().
                filter(relation -> relation.getTodoId() == todoId)
                .toList();
    }

    @Override
    public List<FlowTodoMerge> findByTodoIds(List<Long> todoIds) {
        return cache.values().stream().
                filter(relation -> todoIds.contains(relation.getTodoId()))
                .toList();
    }


    public List<FlowTodoMerge> findAll() {
        return cache.values().stream().toList();
    }
}
