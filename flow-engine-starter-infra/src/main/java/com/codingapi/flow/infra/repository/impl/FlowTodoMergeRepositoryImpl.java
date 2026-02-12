package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.FlowTodoMargeConvertor;
import com.codingapi.flow.infra.entity.FlowTodoMargeEntity;
import com.codingapi.flow.infra.jpa.FlowTodoMargeEntityRepository;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowTodoMergeRepositoryImpl implements FlowTodoMergeRepository {

    private final FlowTodoMargeEntityRepository flowTodoMargeEntityRepository;

    @Override
    public void saveAll(List<FlowTodoMerge> list) {
        for (FlowTodoMerge relation : list){
            this.save(relation);
        }
    }

    private void save(FlowTodoMerge todoMarge){
        FlowTodoMargeEntity entity = FlowTodoMargeConvertor.convert(todoMarge);
        flowTodoMargeEntityRepository.save(entity);
        todoMarge.setId(entity.getId());
    }

    @Override
    public void remove(FlowTodoMerge todoMerge) {
        flowTodoMargeEntityRepository.deleteById(todoMerge.getId());
    }

    @Override
    public List<FlowTodoMerge> findByTodoId(long todoId) {
        return flowTodoMargeEntityRepository.findByTodoId(todoId)
                .stream().map(FlowTodoMargeConvertor::convert).toList();
    }
}
