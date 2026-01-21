package com.codingapi.flow.context;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.ParallelBranchRepository;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class RepositoryContext {

    @Getter
    private final static RepositoryContext instance = new RepositoryContext();

    private RepositoryContext() {
    }

    @Setter
    private FlowRecordRepository flowRecordRepository;
    @Setter
    private FlowOperatorGateway flowOperatorGateway;
    @Setter
    private ParallelBranchRepository parallelBranchRepository;

    public FlowRecord getRecordById(long id) {
        return flowRecordRepository.get(id);
    }

    public List<IFlowOperator> findOperatorByIds(List<Long> ids) {
        return flowOperatorGateway.findByIds(ids);
    }

    public List<IFlowOperator> findOperatorByIds(long... ids) {
        return flowOperatorGateway.findByIds(ids);
    }

    public void saveRecord(FlowRecord flowRecord) {
        flowRecordRepository.save(flowRecord);
    }

    public void saveRecords(List<FlowRecord> flowRecords) {
        flowRecordRepository.saveAll(flowRecords);
    }

    public List<FlowRecord> findRecordsByFromIdAndNodeId(long fromId, String nodeId) {
        return flowRecordRepository.findRecordsByFromIdAndNodeId(fromId, nodeId);
    }

    public List<FlowRecord> findRecordsByProcessId(String processId) {
        return flowRecordRepository.findRecordsByProcessId(processId);
    }

    public int getParallelBranchTriggerCount(String parallelId) {
        return parallelBranchRepository.getTriggerCount(parallelId);
    }

    public void addParallelTriggerCount(String parallelId) {
        parallelBranchRepository.addTriggerCount(parallelId);
    }

    public void clearParallelTriggerCount(String parallelId) {
        parallelBranchRepository.clearTriggerCount(parallelId);
    }
}
