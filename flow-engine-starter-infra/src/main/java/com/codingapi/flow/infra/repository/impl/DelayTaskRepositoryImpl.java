package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.DelayTaskConvertor;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.infra.entity.DelayTaskEntity;
import com.codingapi.flow.infra.jpa.DelayTaskEntityRepository;
import com.codingapi.flow.repository.DelayTaskRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class DelayTaskRepositoryImpl implements DelayTaskRepository {

    private final DelayTaskEntityRepository delayTaskEntityRepository;

    @Override
    public void save(DelayTask task) {
        DelayTaskEntity entity = DelayTaskConvertor.convert(task);
        delayTaskEntityRepository.save(entity);
    }

    @Override
    public void delete(DelayTask task) {
        if(task!=null){
            delayTaskEntityRepository.deleteById(task.getId());
        }
    }

    @Override
    public List<DelayTask> findAll() {
        return delayTaskEntityRepository.findAll().stream().map(DelayTaskConvertor::convert).toList();
    }
}
