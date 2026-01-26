package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.ParallelControlEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

public interface ParallelControlEntityRepository extends FastRepository<ParallelControlEntity,String> {

    ParallelControlEntity getParallelControlEntityById(String id);
}
