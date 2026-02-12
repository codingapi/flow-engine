package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoMarge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTodoMargeRepositoryImpl implements FlowTodoMargeRepository {

    private final Map<Long, FlowTodoMarge> cache = new HashMap<>();

    private void save(FlowTodoMarge relation) {
        if (relation.getId() > 0) {
            cache.put(relation.getId(), relation);
        } else {
            long id = cache.size() + 1;
            relation.setId(id);
            cache.put(id, relation);
        }
    }

    @Override
    public void remove(FlowTodoMarge relation) {
        this.cache.remove(relation.getId());
    }

    @Override
    public void saveAll(List<FlowTodoMarge> relations) {
        for (FlowTodoMarge relation : relations){
            this.save(relation);
        }
    }

    @Override
    public List<FlowTodoMarge> findByTodoId(long todoId) {
        return cache.values().stream().
                filter(relation -> relation.getTodoId() == todoId)
                .toList();
    }
}
