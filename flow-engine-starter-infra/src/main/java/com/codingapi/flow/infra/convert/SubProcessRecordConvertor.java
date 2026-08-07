package com.codingapi.flow.infra.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 子流程执行记录的领域对象与持久化实体互转工具。
 */
public class SubProcessRecordConvertor {

    private SubProcessRecordConvertor() {
    }

    /**
     * 持久化实体转换为领域对象。
     * <p>实例列表从 JSON 文本反序列化，聚合状态由枚举名还原。
     *
     * @param entity 持久化实体
     * @return 领域对象；实体为 null 时返回 null
     */
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

    /**
     * 领域对象转换为持久化实体（新建）。
     *
     * @param record 领域对象
     * @return 持久化实体
     */
    public static SubProcessRecordEntity convert(SubProcessRecord record) {
        SubProcessRecordEntity entity = new SubProcessRecordEntity();
        return convert(record, entity);
    }

    /**
     * 领域对象转换为持久化实体（可复用已有实体）。
     * <p>实例列表序列化为 JSON 文本，聚合状态存为枚举名。
     *
     * @param record 领域对象
     * @param entity 待填充的持久化实体（记录已持久化时携带主键）
     * @return 填充后的持久化实体
     */
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
