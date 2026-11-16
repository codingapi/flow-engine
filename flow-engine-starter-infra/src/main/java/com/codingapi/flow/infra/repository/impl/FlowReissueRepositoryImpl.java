package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.domain.FlowReissueRecord;
import com.codingapi.flow.infra.convert.FlowReissueRecordConvertor;
import com.codingapi.flow.infra.entity.FlowReissueRecordEntity;
import com.codingapi.flow.infra.jpa.FlowReissueRecordEntityRepository;
import com.codingapi.flow.repository.FlowReissueRepository;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 已完成流程二次发起关系仓库的 JPA 实现。
 */
@AllArgsConstructor
public class FlowReissueRepositoryImpl implements FlowReissueRepository {

    private final FlowReissueRecordEntityRepository repository;

    /**
     * 保存二次发起关系：
     * 已持久化的记录（id 大于 0）更新到已有实体，否则新建实体；
     * 保存后回写生成的主键。
     */
    @Override
    public void save(FlowReissueRecord record) {
        FlowReissueRecordEntity entity = record.getId() > 0
                ? repository.findById(record.getId()).orElseGet(FlowReissueRecordEntity::new)
                : new FlowReissueRecordEntity();
        FlowReissueRecordConvertor.convert(record, entity);
        repository.save(entity);
        record.setId(entity.getId());
    }

    @Override
    public List<FlowReissueRecord> findBySourceRecordId(long sourceRecordId) {
        return repository.findBySourceRecordIdOrderById(sourceRecordId).stream()
                .map(FlowReissueRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<FlowReissueRecord> findBySourceProcessId(String sourceProcessId) {
        return repository.findBySourceProcessIdOrderById(sourceProcessId).stream()
                .map(FlowReissueRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<FlowReissueRecord> findByNewProcessId(String newProcessId) {
        return repository.findByNewProcessIdOrderById(newProcessId).stream()
                .map(FlowReissueRecordConvertor::convert)
                .toList();
    }
}
