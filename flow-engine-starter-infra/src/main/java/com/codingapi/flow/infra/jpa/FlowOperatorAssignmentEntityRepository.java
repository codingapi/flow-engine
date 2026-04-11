package com.codingapi.flow.infra.jpa;

import com.codingapi.flow.infra.entity.FlowOperatorAssignmentEntity;
import com.codingapi.springboot.fast.jpa.repository.FastRepository;

import java.util.Optional;

public interface FlowOperatorAssignmentEntityRepository extends FastRepository<FlowOperatorAssignmentEntity, Long> {

    Optional<FlowOperatorAssignmentEntity> findByProcessIdAndNodeId(String processId, String nodeId);
}
