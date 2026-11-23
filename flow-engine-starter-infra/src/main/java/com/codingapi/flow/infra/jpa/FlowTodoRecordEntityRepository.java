package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlowTodoRecordEntityRepository extends FastRepository<FlowTodoRecordEntity,Long> {

    FlowTodoRecordEntity getByTodoKey(String todoKey);

    @Query("from FlowTodoRecordEntity r where r.todoKey in ?1")
    List<FlowTodoRecordEntity> findByKeys(List<String> keys);


    @Query("from FlowTodoRecordEntity r where r.currentOperatorId = ?1")
    Page<FlowTodoRecordEntity> findTodoRecordPage(long currentOperatorId, PageRequest pageRequest);

    /**
     * 按 {@code (审批人, 身份)} 分页查询待办（issue #288）。
     *
     * <p>{@code coalesce(..., '')} 是刻意为之：历史数据的身份列是 NULL，
     * 若直接写 {@code identity = :identity}，升级后所有存量待办都会从用户列表中消失。
     * 归一化后「NULL 与空串」都命中默认身份，与引擎内的 {@code OperatorIdentity} 约定一致。
     *
     * @param currentOperatorId 当前审批人 id
     * @param identity          身份标识（已归一化，默认身份为空串）
     * @param pageRequest       分页
     * @return 待办分页
     */
    @Query("from FlowTodoRecordEntity r where r.currentOperatorId = :operatorId " +
            "and coalesce(r.currentOperatorIdentity, '') = :identity")
    Page<FlowTodoRecordEntity> findTodoRecordPageByIdentity(@Param("operatorId") long currentOperatorId,
                                                            @Param("identity") String identity,
                                                            PageRequest pageRequest);
}
