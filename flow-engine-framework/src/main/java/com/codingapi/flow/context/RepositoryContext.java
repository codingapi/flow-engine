package com.codingapi.flow.context;

import com.codingapi.flow.delay.DelayTask;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.DelayTaskRepository;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.ParallelBranchRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import lombok.Getter;

import java.util.List;

public class RepositoryContext {

    @Getter
    private final static RepositoryContext instance = new RepositoryContext();

    private RepositoryContext() {
    }

    @Getter
    private WorkflowBackupRepository workflowBackupRepository;
    @Getter
    private FlowRecordRepository flowRecordRepository;
    @Getter
    private FlowOperatorGateway flowOperatorGateway;
    @Getter
    private ParallelBranchRepository parallelBranchRepository;
    @Getter
    private DelayTaskRepository delayTaskRepository;

    /**
     * 是否已经注册成功
     */
    public boolean isRegistered() {
        return parallelBranchRepository != null
                && delayTaskRepository != null
                && workflowBackupRepository != null
                && flowRecordRepository != null
                && flowOperatorGateway != null;
    }

    public void register(WorkflowBackupRepository workflowBackupRepository, FlowRecordRepository flowRecordRepository, FlowOperatorGateway flowOperatorGateway, ParallelBranchRepository parallelBranchRepository, DelayTaskRepository delayTaskRepository) {
        this.workflowBackupRepository = workflowBackupRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.parallelBranchRepository = parallelBranchRepository;
        this.delayTaskRepository = delayTaskRepository;
    }

    public FlowRecord getRecordById(long id) {
        return flowRecordRepository.get(id);
    }

    public List<IFlowOperator> findOperatorByIds(List<Long> ids) {
        return flowOperatorGateway.findByIds(ids);
    }

    public List<IFlowOperator> findOperatorByIds(long... ids) {
        return flowOperatorGateway.findByIds(ids);
    }

    public void saveDelayTask(DelayTask delayTask) {
        delayTaskRepository.save(delayTask);
    }

    public void deleteDelayTask(DelayTask delayTask) {
        delayTaskRepository.delete(delayTask);
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

    public List<DelayTask> findDelayTasks() {
        return delayTaskRepository.findAll();
    }


}
