package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoMerge;

import java.util.List;

public interface FlowTodoMergeRepository {

    void saveAll(List<FlowTodoMerge> list);

    void remove(FlowTodoMerge todoMerge);

    List<FlowTodoMerge> findByTodoId(long todoId);

}
