package com.codingapi.flow.infra.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;

import java.util.ArrayList;
import java.util.List;

public class SubProcessRecordConvertor {

    private SubProcessRecordConvertor() {
    }

    public static SubProcessRecord convert(SubProcessRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        List<SubProcessRecord.Instance> instances = JSON.parseArray(
                entity.getInstances(), SubProcessRecord.Instance.class, JSONReader.Feature.FieldBased);
        return new SubProcessRecord(
                entity.getId(),
                entity.getGroupId(),
                entity.getParentProcessId(),
                entity.getParentRecordId(),
                entity.getParentWorkRuntimeId(),
                entity.getNodeId(),
                entity.getTotalCount(),
                new ArrayList<>(instances),
                SubProcessRecord.State.valueOf(entity.getState()),
                entity.getCreateTime(),
                entity.getFinishTime());
    }

    public static SubProcessRecordEntity convert(SubProcessRecord record) {
        SubProcessRecordEntity entity = new SubProcessRecordEntity();
        return convert(record, entity);
    }

    public static SubProcessRecordEntity convert(SubProcessRecord record, SubProcessRecordEntity entity) {
        if (record.getId() > 0) {
            entity.setId(record.getId());
        }
        entity.setGroupId(record.getGroupId());
        entity.setParentProcessId(record.getParentProcessId());
        entity.setParentRecordId(record.getParentRecordId());
        entity.setParentWorkRuntimeId(record.getParentWorkRuntimeId());
        entity.setNodeId(record.getNodeId());
        entity.setTotalCount(record.getTotalCount());
        entity.setInstances(JSON.toJSONString(record.getInstances()));
        entity.setState(record.getState().name());
        entity.setCreateTime(record.getCreateTime());
        entity.setFinishTime(record.getFinishTime());
        return entity;
    }
}
