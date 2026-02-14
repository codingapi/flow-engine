package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.infra.entity.UrgeIntervalEntity;

public class UrgeIntervalConvertor {

    public static UrgeIntervalEntity convert(UrgeInterval interval) {
        if (interval == null) {
            return null;
        }
        UrgeIntervalEntity entity = new UrgeIntervalEntity();
        if(interval.getId()>0) {
            entity.setId(interval.getId());
        }
        entity.setProcessId(interval.getProcessId());
        entity.setRecordId(interval.getRecordId());
        entity.setCreateTime(interval.getCreateTime());
        return entity;
    }

    public static UrgeInterval convert(UrgeIntervalEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UrgeInterval(entity.getId(), entity.getProcessId(), entity.getRecordId(), entity.getCreateTime());
    }
}
