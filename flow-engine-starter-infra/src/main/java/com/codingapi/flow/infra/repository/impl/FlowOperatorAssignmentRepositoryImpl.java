package com.codingapi.flow.infra.repository.impl;

import com.alibaba.fastjson2.JSON;
import com.codingapi.flow.infra.entity.FlowOperatorAssignmentEntity;
import com.codingapi.flow.infra.jpa.FlowOperatorAssignmentEntityRepository;
import com.codingapi.flow.repository.FlowOperatorAssignmentRepository;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class FlowOperatorAssignmentRepositoryImpl implements FlowOperatorAssignmentRepository {

    private final FlowOperatorAssignmentEntityRepository jpa;

    @Override
    public void save(String processId, String nodeId, List<Long> operatorIds) {
        Optional<FlowOperatorAssignmentEntity> existing = jpa.findByProcessIdAndNodeId(processId, nodeId);
        FlowOperatorAssignmentEntity entity = existing.orElseGet(FlowOperatorAssignmentEntity::new);
        entity.setProcessId(processId);
        entity.setNodeId(nodeId);
        entity.setOperatorIds(JSON.toJSONString(operatorIds));
        jpa.save(entity);
    }

    @Override
    public List<Long> findOperatorIds(String processId, String nodeId) {
        return jpa.findByProcessIdAndNodeId(processId, nodeId)
                .map(e -> JSON.parseArray(e.getOperatorIds(), Long.class))
                .orElse(Collections.emptyList());
    }
}
