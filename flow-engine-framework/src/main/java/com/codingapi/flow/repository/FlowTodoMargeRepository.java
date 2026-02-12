package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoMarge;

import java.util.List;

public interface FlowTodoMargeRepository {

    void saveAll(List<FlowTodoMarge> relations);

    void remove(FlowTodoMarge relation);

    List<FlowTodoMarge> findByTodoId(long todoId);

}
