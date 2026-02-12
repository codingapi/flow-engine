package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowTodoMargeEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

import java.util.List;

public interface FlowTodoMargeEntityRepository extends FastRepository<FlowTodoMargeEntity,Long> {

    List<FlowTodoMargeEntity> findByTodoId(long todoId);
}
