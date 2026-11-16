package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowReissueRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

import java.util.List;

/**
 * 已完成流程二次发起关系实体的 JPA 仓储接口。
 */
public interface FlowReissueRecordEntityRepository extends FastRepository<FlowReissueRecordEntity, Long> {

    /**
     * 按源流程记录id查询，按记录id升序。
     *
     * @param sourceRecordId 源流程记录id
     * @return 二次发起关系实体列表
     */
    List<FlowReissueRecordEntity> findBySourceRecordIdOrderById(long sourceRecordId);

    /**
     * 按源流程id查询，按记录id升序。
     *
     * @param sourceProcessId 源流程id
     * @return 二次发起关系实体列表
     */
    List<FlowReissueRecordEntity> findBySourceProcessIdOrderById(String sourceProcessId);

    /**
     * 按新流程id查询，按记录id升序。
     *
     * @param newProcessId 新流程id
     * @return 二次发起关系实体列表
     */
    List<FlowReissueRecordEntity> findByNewProcessIdOrderById(String newProcessId);
}
