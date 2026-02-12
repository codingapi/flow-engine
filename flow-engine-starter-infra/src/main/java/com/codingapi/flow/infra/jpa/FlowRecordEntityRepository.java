package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FlowRecordEntityRepository extends FastRepository<FlowRecordEntity, Long> {

    FlowRecordEntity getFlowRecordEntityById(long id);

    @Query("from FlowRecordEntity r where r.processId = ?1")
    List<FlowRecordEntity> findProcessIdRecords(String processId);

    @Query("from FlowRecordEntity r where r.fromId = ?1 and r.nodeId =?2 and r.revoked = false")
    List<FlowRecordEntity> findCurrentNodeRecords(long fromId, String nodeId);

    @Query("from FlowRecordEntity r where r.processId = ?1 and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    List<FlowRecordEntity> findTodoRecords(String processId);

    @Query("from FlowRecordEntity r where r.processId = ?1 and r.fromId >=?2 and r.hidden=false and r.revoked = false")
    List<FlowRecordEntity> findAfterRecords(String processId, long fromId);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    Page<FlowRecordEntity> findTodoRecordPage(long operatorId, PageRequest pageRequest);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 1 and r.hidden=false and r.revoked = false) ")
    Page<FlowRecordEntity> findDoneRecordPage(long operatorId, PageRequest pageRequest);

    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and r.notify = true and r.hidden=false and r.revoked = false ")
    Page<FlowRecordEntity> findNotifyRecordPage(long operatorId, PageRequest pageRequest);


}
