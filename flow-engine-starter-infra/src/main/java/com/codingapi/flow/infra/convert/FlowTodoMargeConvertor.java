package com.codingapi.flow.infra.convert;

import com.codingapi.flow.infra.entity.FlowTodoMargeEntity;
import com.codingapi.flow.record.FlowTodoMerge;

public class FlowTodoMargeConvertor {

    public static FlowTodoMerge convert(FlowTodoMargeEntity entity) {
        if(entity==null){
            return null;
        }
        return new FlowTodoMerge(
                entity.getId(),
                entity.getTodoId(),
                entity.getRecordId(),
                entity.getCreateTime()
        );
    }

    public static FlowTodoMargeEntity convert(FlowTodoMerge marge) {
        if(marge==null){
            return null;
        }
        FlowTodoMargeEntity entity = new FlowTodoMargeEntity();
        if(marge.getId()>0) {
            entity.setId(marge.getId());
        }
        entity.setTodoId(marge.getTodoId());
        entity.setRecordId(marge.getRecordId());
        entity.setCreateTime(marge.getCreateTime());
        return entity;
    }
}
