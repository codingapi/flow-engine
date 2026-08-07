package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * 子流程执行记录实体的 JPA 仓储接口。
 */
public interface SubProcessRecordEntityRepository extends FastRepository<SubProcessRecordEntity, Long> {

    /**
     * 按父流程中子流程节点的执行记录id查询，加悲观写锁防止子流程结果并发判定。
     *
     * @param parentRecordId 父流程中子流程节点的执行记录id
     * @return 子流程执行记录实体列表，按记录id升序
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("from SubProcessRecordEntity r where r.parentRecordId = ?1 order by r.id")
    List<SubProcessRecordEntity> findForUpdateByParentRecordId(long parentRecordId);

    /**
     * 按父流程（主流程）的流程id查询，按记录id升序。
     *
     * @param parentProcessId 父流程（主流程）的流程id
     * @return 子流程执行记录实体列表
     */
    List<SubProcessRecordEntity> findByParentProcessIdOrderById(String parentProcessId);

    /**
     * 按父流程（主流程）的流程id与节点id查询，按记录id升序。
     *
     * @param parentProcessId 父流程（主流程）的流程id
     * @param nodeId          父流程中子流程节点的节点id
     * @return 子流程执行记录实体列表
     */
    List<SubProcessRecordEntity> findByParentProcessIdAndNodeIdOrderById(
            String parentProcessId, String nodeId);

}
