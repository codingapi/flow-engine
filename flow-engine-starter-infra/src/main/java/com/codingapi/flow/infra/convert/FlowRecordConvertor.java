package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.convert.MapConvertor;
import com.codingapi.flow.record.FlowRecord;

public class FlowRecordConvertor {

    private final static MapConvertor mapConvertor = new MapConvertor();

    public static FlowRecord convert(FlowRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FlowRecord(entity.getId(),
                entity.getWorkBackupId(),
                entity.getWorkCode(),
                entity.getNodeId(),
                entity.getNodeType(),
                entity.getFromId(),
                mapConvertor.convertToEntityAttribute(entity.getFormData()),
                entity.getTitle(),
                entity.getReadTime(),
                entity.getProcessId(),
                entity.getActionId(),
                entity.getActionType(),
                entity.getAdvice(),
                entity.getSignKey(),
                entity.getCurrentOperatorId(),
                entity.getCurrentOperatorName(),
                entity.getForwardOperatorId(),
                entity.getForwardOperatorName(),
                entity.getReturnNodeId(),
                entity.getNodeOrder(),
                entity.getHidden(),
                entity.getRevoked(),
                entity.getNotify(),
                entity.getRecordState(),
                entity.getFlowState(),
                entity.getUpdateTime(),
                entity.getCreateTime(),
                entity.getFinishTime(),
                entity.getReadable(),
                entity.getCreateOperatorId(),
                entity.getCreateOperatorName(),
                entity.getErrMessage(),
                entity.getTimeoutTime(),
                entity.getMergeable(),
                entity.getInterferedOperatorId(),
                entity.getInterferedOperatorName(),
                entity.getDelegateId(),
                entity.getParallelId(),
                entity.getParallelBranchNodeId(),
                entity.getParallelBranchTotal());
    }

    public static FlowRecordEntity convert(FlowRecord record) {
        if (record == null) {
            return null;
        }
        FlowRecordEntity entity = new FlowRecordEntity();
        entity.setId(record.getId());
        entity.setWorkBackupId(record.getWorkBackupId());
        entity.setWorkCode(record.getWorkCode());
        entity.setNodeId(record.getNodeId());
        entity.setNodeType(record.getNodeType());
        entity.setFromId(record.getFromId());
        entity.setFormData(mapConvertor.convertToDatabaseColumn(record.getFormData()));
        entity.setTitle(record.getTitle());
        entity.setReadTime(record.getReadTime());
        entity.setProcessId(record.getProcessId());
        entity.setActionId(record.getActionId());
        entity.setActionType(record.getActionType());
        entity.setAdvice(record.getAdvice());
        entity.setSignKey(record.getSignKey());
        entity.setCurrentOperatorId(record.getCurrentOperatorId());
        entity.setCurrentOperatorName(record.getCurrentOperatorName());
        entity.setForwardOperatorId(record.getForwardOperatorId());
        entity.setForwardOperatorName(record.getForwardOperatorName());
        entity.setReturnNodeId(record.getReturnNodeId());
        entity.setNodeOrder(record.getNodeOrder());
        entity.setHidden(record.isHidden());
        entity.setRevoked(record.isRevoked());
        entity.setNotify(record.isNotify());
        entity.setRecordState(record.getRecordState());
        entity.setFlowState(record.getFlowState());
        entity.setUpdateTime(record.getUpdateTime());
        entity.setCreateTime(record.getCreateTime());
        entity.setFinishTime(record.getFinishTime());
        entity.setReadable(record.isReadable());
        entity.setCreateOperatorId(record.getCreateOperatorId());
        entity.setCreateOperatorName(record.getCreateOperatorName());
        entity.setErrMessage(record.getErrMessage());
        entity.setTimeoutTime(record.getTimeoutTime());
        entity.setMergeable(record.isMergeable());
        entity.setInterferedOperatorId(record.getInterferedOperatorId());
        entity.setInterferedOperatorName(record.getInterferedOperatorName());
        entity.setDelegateId(record.getDelegateId());
        entity.setParallelId(record.getParallelId());
        entity.setParallelBranchNodeId(record.getParallelBranchNodeId());
        entity.setParallelBranchTotal(record.getParallelBranchTotal());
        return entity;
    }


}
