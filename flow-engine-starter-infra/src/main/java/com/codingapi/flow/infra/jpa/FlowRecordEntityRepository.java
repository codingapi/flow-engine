package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

import java.util.List;

public interface FlowRecordEntityRepository extends FastRepository<FlowRecordEntity,Long> {

    FlowRecordEntity getFlowRecordEntityById(long id);

    List<FlowRecordEntity> findFlowRecordEntityByProcessId(String processId);

    List<FlowRecordEntity> findFlowRecordEntityByFromIdAndNodeIdAndRevoked(long fromId, String nodeId, boolean revoked);

    List<FlowRecordEntity> findFlowRecordEntityByProcessIdAndRecordStateAndFlowStateAndHiddenAndRevoked(String processId,
                                                                                   int recordState,
                                                                                   int flowState,
                                                                                   boolean hidden,
                                                                                   boolean revoked);

    List<FlowRecordEntity> findFlowRecordEntityByProcessIdAndFromIdGreaterThanEqual(String processId, long fromId);

}
