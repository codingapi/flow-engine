package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FlowRecordEntityRepository extends FastRepository<FlowRecordEntity, Long> {

    FlowRecordEntity getFlowRecordEntityById(long id);

    List<FlowRecordEntity> findFlowRecordEntityByProcessId(String processId);

    List<FlowRecordEntity> findFlowRecordEntityByFromIdAndNodeIdAndRevoked(long fromId, String nodeId, boolean revoked);

    List<FlowRecordEntity> findFlowRecordEntityByProcessIdAndRecordStateAndFlowStateAndHiddenAndRevoked(String processId,
                                                                                                        int recordState,
                                                                                                        int flowState,
                                                                                                        boolean hidden,
                                                                                                        boolean revoked);

    List<FlowRecordEntity> findFlowRecordEntityByProcessIdAndFromIdGreaterThanEqual(String processId, long fromId);


    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 0 and r.flowState = 0 and r.hidden=false and r.revoked = false)")
    Page<FlowRecordEntity> findTodoPage(long operatorId, PageRequest pageRequest);


    @Query("from FlowRecordEntity r where r.currentOperatorId = ?1 and (r.recordState = 1 and r.hidden=false and r.revoked = false) ")
    Page<FlowRecordEntity> findDonePage(long operatorId, PageRequest pageRequest);


}
