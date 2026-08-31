package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowTodoMerge;

import java.util.List;

public interface FlowTodoMergeRepository {

    void saveAll(List<FlowTodoMerge> list);

    void delete(FlowTodoMerge todoMerge);

    List<FlowTodoMerge> findByTodoId(long todoId);

    /**
     * 按待办id批量查询合并关系
     * @param todoIds 待办id列表
     * @return 合并关系列表
     */
    List<FlowTodoMerge> findByTodoIds(List<Long> todoIds);

}
