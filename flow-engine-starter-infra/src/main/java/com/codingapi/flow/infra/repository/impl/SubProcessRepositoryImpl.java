package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.infra.convert.SubProcessRecordConvertor;
import com.codingapi.flow.infra.entity.SubProcessRecordEntity;
import com.codingapi.flow.infra.jpa.SubProcessRecordEntityRepository;
import com.codingapi.flow.repository.SubProcessRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class SubProcessRepositoryImpl implements SubProcessRepository {

    private final SubProcessRecordEntityRepository repository;

    @Override
    public void save(SubProcessRecord record) {
        SubProcessRecordEntity entity = record.getId() > 0
                ? repository.findById(record.getId()).orElseGet(SubProcessRecordEntity::new)
                : new SubProcessRecordEntity();
        SubProcessRecordConvertor.convert(record, entity);
        repository.save(entity);
        record.setId(entity.getId());
    }

    @Override
    public List<SubProcessRecord> findByParentRecordId(long parentRecordId) {
        return repository.findForUpdateByParentRecordId(parentRecordId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<SubProcessRecord> findByParentProcessId(String parentProcessId) {
        return repository.findByParentProcessIdOrderById(parentProcessId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<SubProcessRecord> findByParentProcessIdAndNodeId(String parentProcessId, String nodeId) {
        return repository.findByParentProcessIdAndNodeIdOrderById(parentProcessId, nodeId).stream()
                .map(SubProcessRecordConvertor::convert)
                .toList();
    }

}
