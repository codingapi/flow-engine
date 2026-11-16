package com.codingapi.flow.infra.convert;

import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.infra.entity.FlowReissueRecordEntity;

/**
 * 已完成流程二次发起关系的领域对象与持久化实体互转工具。
 */
public class FlowReissueRecordConvertor {

    private FlowReissueRecordConvertor() {
    }

    /**
     * 持久化实体转换为领域对象。
     *
     * @param entity 持久化实体
     * @return 领域对象；实体为 null 时返回 null
     */
    public static FlowReissueRecord convert(FlowReissueRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FlowReissueRecord(
                entity.getId(),
                entity.getSourceRecordId(),
                entity.getSourceProcessId(),
                entity.getNewRecordId(),
                entity.getNewProcessId(),
                entity.getWorkCode(),
                entity.getWorkRuntimeId(),
                entity.getOperatorId(),
                entity.getOperatorName(),
                entity.getCreateTime(),
                entity.getAdvice());
    }

    /**
     * 领域对象转换为持久化实体（新建）。
     *
     * @param record 领域对象
     * @return 持久化实体
     */
    public static FlowReissueRecordEntity convert(FlowReissueRecord record) {
        FlowReissueRecordEntity entity = new FlowReissueRecordEntity();
        return convert(record, entity);
    }

    /**
     * 领域对象转换为持久化实体（可复用已有实体）。
     *
     * @param record 领域对象
     * @param entity 待填充的持久化实体（记录已持久化时携带主键）
     * @return 填充后的持久化实体
     */
    public static FlowReissueRecordEntity convert(FlowReissueRecord record, FlowReissueRecordEntity entity) {
        if (record.getId() > 0) {
            entity.setId(record.getId());
        }
        entity.setSourceRecordId(record.getSourceRecordId());
        entity.setSourceProcessId(record.getSourceProcessId());
        entity.setNewRecordId(record.getNewRecordId());
        entity.setNewProcessId(record.getNewProcessId());
        entity.setWorkCode(record.getWorkCode());
        entity.setWorkRuntimeId(record.getWorkRuntimeId());
        entity.setOperatorId(record.getOperatorId());
        entity.setOperatorName(record.getOperatorName());
        entity.setCreateTime(record.getCreateTime());
        entity.setAdvice(record.getAdvice());
        return entity;
    }
}
