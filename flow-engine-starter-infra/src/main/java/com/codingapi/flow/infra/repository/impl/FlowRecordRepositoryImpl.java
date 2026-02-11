package com.codingapi.flow.infra.repository.impl;

import com.codingapi.flow.infra.convert.FlowRecordConvertor;
import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.jpa.FlowRecordEntityRepository;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowRecordRepositoryImpl implements FlowRecordRepository {

    private final FlowRecordEntityRepository flowRecordEntityRepository;

    @Override
    public FlowRecord get(long id) {
        return FlowRecordConvertor.convert(flowRecordEntityRepository.getFlowRecordEntityById(id));
    }

    @Override
    public void save(FlowRecord flowRecord) {
        FlowRecordEntity entity = FlowRecordConvertor.convert(flowRecord);
        flowRecordEntityRepository.save(entity);
        flowRecord.setId(entity.getId());
    }

    @Override
    public void saveAll(List<FlowRecord> flowRecords) {
        for (FlowRecord flowRecord : flowRecords){
            this.save(flowRecord);
        }
    }

    @Override
    public void delete(FlowRecord flowRecord) {
        flowRecordEntityRepository.deleteById(flowRecord.getId());
    }

    @Override
    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return flowRecordEntityRepository.findCurrentNodeRecords(fromId, nodeId)
                .stream()
                .map(FlowRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<FlowRecord> findProcessRecords(String processId) {
        return flowRecordEntityRepository.findProcessIdRecords(processId)
                .stream()
                .map(FlowRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<FlowRecord> findTodoRecords(String processId) {
        return flowRecordEntityRepository.findTodoRecords(processId)
                .stream()
                .map(FlowRecordConvertor::convert)
                .toList();
    }

    @Override
    public List<FlowRecord> findAfterRecords(String processId, long fromId) {
        return flowRecordEntityRepository.findAfterRecords(processId, fromId)
                .stream()
                .map(FlowRecordConvertor::convert)
                .toList();
    }
}
