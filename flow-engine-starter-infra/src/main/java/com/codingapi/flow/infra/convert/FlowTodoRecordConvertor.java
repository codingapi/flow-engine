package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.record.FlowTodoRecord;

public class FlowTodoRecordConvertor {

    public static FlowTodoRecord convert(FlowTodoRecordEntity entity){
        if(entity==null){
            return null;
        }
        return new FlowTodoRecord(
                entity.getId(),
                entity.getProcessId(),
                entity.getWorkBackupId(),
                entity.getWorkCode(),
                entity.getNodeId(),
                entity.getNodeType(),
                entity.getNodeName(),
                entity.getTitle(),
                entity.getReadTime(),
                entity.getCurrentOperatorId(),
                entity.getCurrentOperatorName(),
                entity.getCreateTime(),
                entity.getCreateOperatorId(),
                entity.getCreateOperatorName(),
                entity.getTodoKey(),
                entity.getMargeCount(),
                entity.getMergeable(),
                entity.getRecordId(),
                entity.getTimeoutTime()
        );
    }


    public static FlowTodoRecordEntity convert(FlowTodoRecord record){
        if(record==null){
            return null;
        }

        FlowTodoRecordEntity entity = new FlowTodoRecordEntity();
        if(record.getId()>0) {
            entity.setId(record.getId());
        }
        entity.setProcessId(record.getProcessId());
        entity.setWorkBackupId(record.getWorkBackupId());
        entity.setWorkCode(record.getWorkCode());
        entity.setNodeId(record.getNodeId());
        entity.setNodeType(record.getNodeType());
        entity.setNodeName(record.getNodeName());
        entity.setTitle(record.getTitle());
        entity.setReadTime(record.getReadTime());
        entity.setCurrentOperatorId(record.getCurrentOperatorId());
        entity.setCurrentOperatorName(record.getCurrentOperatorName());
        entity.setCreateTime(record.getCreateTime());
        entity.setCreateOperatorId(record.getCreateOperatorId());
        entity.setCreateOperatorName(record.getCreateOperatorName());
        entity.setTodoKey(record.getTodoKey());
        entity.setMargeCount(record.getMargeCount());
        entity.setMergeable(record.isMergeable());
        entity.setRecordId(record.getRecordId());
        entity.setTimeoutTime(record.getTimeoutTime());
        return entity;
    }
}
