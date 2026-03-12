package com.codingapi.flow.mock;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.mock.repository.*;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;

import java.util.List;

@Getter
public class MockRepositoryHolder implements IRepositoryHolder {

    private final DelayTaskRepository delayTaskRepository;
    private final ParallelBranchRepository parallelBranchRepository;
    private final UrgeIntervalRepository urgeIntervalRepository;
    private final FlowRecordRepository flowRecordRepository;
    private final FlowTodoMergeRepository flowTodoMergeRepository;
    private final FlowTodoRecordRepository flowTodoRecordRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowRuntimeRepository workflowRuntimeRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final FlowOperatorGateway flowOperatorGateway;

    public MockRepositoryHolder(FlowOperatorGateway flowOperatorGateway) {
        this.flowOperatorGateway = flowOperatorGateway;
        this.delayTaskRepository = new DelayTaskRepositoryMockImpl();
        this.flowRecordRepository = new FlowRecordRepositoryMockImpl();
        this.flowTodoMergeRepository = new FlowTodoMergeRepositoryMockImpl();
        this.flowTodoRecordRepository = new FlowTodoRecordRepositoryMockImpl();
        this.parallelBranchRepository = new ParallelBranchRepositoryMockImpl();
        this.urgeIntervalRepository = new UrgeIntervalRepositoryMockImpl();
        this.workflowRepository = new WorkflowRepositoryMockImpl();
        this.workflowRuntimeRepository = new WorkflowRuntimeRepositoryMockImpl();
        this.workflowVersionRepository = new WorkflowVersionRepositoryMockImpl();
        this.flowRecordService = new FlowRecordService(flowTodoRecordRepository, flowTodoMergeRepository, flowRecordRepository);
        this.workflowService = new WorkflowService(workflowVersionRepository, workflowRepository, workflowRuntimeRepository);
    }

    @Override
    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    @Override
    public FlowRecordService getFlowRecordService() {
        return flowRecordService;
    }

    @Override
    public FlowOperatorGateway getFlowOperatorGateway() {
        return flowOperatorGateway;
    }

    @Override
    public FlowDelayTriggerService createDelayTriggerService(DelayTask task) {
        return new FlowDelayTriggerService(task, this);
    }

    @Override
    public FlowActionService createFlowActionService(FlowSession flowSession) {
        return new FlowActionService(flowSession.toActionRequest(), this);
    }

    @Override
    public FlowService createFlowService() {
        return new FlowService(this);
    }

    @Override
    public FlowRecord getRecordById(long recordId) {
        return this.flowRecordService.getFlowRecord(recordId);
    }

    @Override
    public List<IFlowOperator> findOperatorByIds(List<Long> ids) {
        return this.flowOperatorGateway.findByIds(ids);
    }

    @Override
    public IFlowOperator getOperatorById(long id) {
        return this.flowOperatorGateway.get(id);
    }

    @Override
    public void saveDelayTask(DelayTask delayTask) {
        this.delayTaskRepository.save(delayTask);
    }

    @Override
    public void deleteDelayTask(DelayTask delayTask) {
        this.delayTaskRepository.delete(delayTask);
    }

    @Override
    public void saveRecords(List<FlowRecord> flowRecords) {
        this.flowRecordService.saveFlowRecords(flowRecords);
    }

    @Override
    public void saveRecord(FlowRecord flowRecord) {
        this.flowRecordService.saveFlowRecord(flowRecord);
    }

    @Override
    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return this.flowRecordService.findFlowRecordCurrentNodeRecords(fromId, nodeId);
    }

    @Override
    public List<FlowRecord> findProcessRecords(String processId) {
        return this.flowRecordService.findFlowRecordByProcessId(processId);
    }

    @Override
    public List<FlowRecord> findAfterRecords(String processId, long currentId) {
        return this.flowRecordService.findFlowRecordAfterRecords(processId, currentId);
    }

    @Override
    public int getParallelBranchTriggerCount(String parallelId) {
        return this.parallelBranchRepository.getTriggerCount(parallelId);
    }

    @Override
    public void addParallelTriggerCount(String parallelId) {
        this.parallelBranchRepository.addTriggerCount(parallelId);
    }

    @Override
    public void clearParallelTriggerCount(String parallelId) {
        this.parallelBranchRepository.clearTriggerCount(parallelId);
    }

    @Override
    public void saveUrgeInterval(UrgeInterval interval) {
        this.urgeIntervalRepository.save(interval);
    }

    @Override
    public UrgeInterval getLatestUrgeInterval(String processId, long recordId) {
        return this.urgeIntervalRepository.getLatest(processId, recordId);
    }

    @Override
    public List<DelayTask> findDelayTasks() {
        return this.delayTaskRepository.findAll();
    }
}
