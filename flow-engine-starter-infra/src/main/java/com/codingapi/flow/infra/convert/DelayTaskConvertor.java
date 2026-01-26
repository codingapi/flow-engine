package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.infra.entity.DelayTaskEntity;

public class DelayTaskConvertor {

    public static DelayTask convert(DelayTaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DelayTask(entity.getId(), entity.getCreateTime(), entity.getTriggerTime(), entity.getCurrentRecordId(), entity.getWorkCode(), entity.getDelayNodeId());
    }

    public static DelayTaskEntity convert(DelayTask task) {
        if (task == null) {
            return null;
        }
        DelayTaskEntity entity = new DelayTaskEntity();
        entity.setId(task.getId());
        entity.setCreateTime(task.getCreateTime());
        entity.setTriggerTime(task.getTriggerTime());
        entity.setCurrentRecordId(task.getCurrentRecordId());
        entity.setWorkCode(task.getWorkCode());
        entity.setDelayNodeId(task.getDelayNodeId());
        return entity;
    }
}
