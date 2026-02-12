package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoMerge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTodoMergeRepositoryImpl implements FlowTodoMergeRepository {

    private final Map<Long, FlowTodoMerge> cache = new HashMap<>();

    private void save(FlowTodoMerge relation) {
        if (relation.getId() > 0) {
            cache.put(relation.getId(), relation);
        } else {
            long id = cache.size() + 1;
            relation.setId(id);
            cache.put(id, relation);
        }
    }

    @Override
    public void remove(FlowTodoMerge todoMerge) {
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


    public List<FlowTodoMerge> findAll() {
        return cache.values().stream().toList();
    }
}
