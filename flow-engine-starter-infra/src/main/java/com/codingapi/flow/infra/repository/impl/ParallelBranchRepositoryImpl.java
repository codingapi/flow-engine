package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.entity.ParallelControlEntity;
import com.codingapi.flow.infra.jpa.ParallelControlEntityRepository;
import com.codingapi.flow.repository.ParallelBranchRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ParallelBranchRepositoryImpl implements ParallelBranchRepository {

    private final ParallelControlEntityRepository parallelControlEntityRepository;

    @Override
    public int getTriggerCount(String parallelId) {
        ParallelControlEntity entity = parallelControlEntityRepository.getParallelControlEntityById(parallelId);
        if(entity != null){
            return entity.getCount();
        }
        return 0;
    }

    @Override
    public void addTriggerCount(String parallelId) {
        ParallelControlEntity entity = parallelControlEntityRepository.getParallelControlEntityById(parallelId);
        if(entity != null){
            entity.setCount(entity.getCount() + 1);
            parallelControlEntityRepository.save(entity);
        }{
            ParallelControlEntity newEntity = new ParallelControlEntity();
            newEntity.setId(parallelId);
            newEntity.setCount(1);
            parallelControlEntityRepository.save(newEntity);
        }
    }

    @Override
    public void clearTriggerCount(String parallelId) {
        parallelControlEntityRepository.deleteById(parallelId);
    }
}
