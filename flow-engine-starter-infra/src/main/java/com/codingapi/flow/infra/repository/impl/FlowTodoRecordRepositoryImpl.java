package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.FlowTodoRecordConvertor;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.infra.jpa.FlowTodoRecordEntityRepository;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowTodoRecordRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowTodoRecordRepositoryImpl implements FlowTodoRecordRepository {

    private final FlowTodoRecordEntityRepository flowTodoRecordEntityRepository;

    @Override
    public void saveAll(List<FlowTodoRecord> margeRecords) {
        for (FlowTodoRecord record : margeRecords){
            this.save(record);
        }
    }

    @Override
    public FlowTodoRecord getByMergeKey(String key) {
        return FlowTodoRecordConvertor.convert(flowTodoRecordEntityRepository.getByMergeKey(key));
    }

    @Override
    public void delete(FlowTodoRecord margeRecord) {
        flowTodoRecordEntityRepository.deleteById(margeRecord.getId());
    }

    @Override
    public void save(FlowTodoRecord margeRecord) {
        FlowTodoRecordEntity entity = FlowTodoRecordConvertor.convert(margeRecord);
        flowTodoRecordEntityRepository.save(entity);
        margeRecord.setId(entity.getId());
    }
}
