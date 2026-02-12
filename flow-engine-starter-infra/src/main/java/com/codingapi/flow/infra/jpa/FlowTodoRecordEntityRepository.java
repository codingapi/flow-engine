package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;

public interface FlowTodoRecordEntityRepository extends FastRepository<FlowTodoRecordEntity,Long> {

    FlowTodoRecordEntity getByMergeKey(String mergeKey);


    @Query("from FlowTodoRecordEntity r where r.currentOperatorId = ?1")
    Page<FlowTodoRecordEntity> findTodoRecordPage(long currentOperatorId, PageRequest pageRequest);
}
