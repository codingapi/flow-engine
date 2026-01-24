package com.codingapi.flow.context;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;

import java.util.List;

public class RepositoryHolderContext {

    @Getter
    private final static RepositoryHolderContext instance = new RepositoryHolderContext();

    private RepositoryHolderContext() {
    }

    @Getter
    private WorkflowRepository workflowRepository;
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
    @Getter
    private UrgeIntervalRepository urgeIntervalRepository;

    /**
     * 是否已经注册成功
     */
    public boolean isRegistered() {
        return parallelBranchRepository != null
                && delayTaskRepository != null
                && workflowBackupRepository != null
                && flowRecordRepository != null
                && flowOperatorGateway != null
                && workflowRepository != null
                && urgeIntervalRepository != null;
    }


    public void verify() {
        if (!isRegistered()) {
            throw FlowConfigException.repositoryNotRegistered();
        }
    }

    public void register(WorkflowRepository workflowRepository,
                         WorkflowBackupRepository workflowBackupRepository,
                         FlowRecordRepository flowRecordRepository,
                         FlowOperatorGateway flowOperatorGateway,
                         ParallelBranchRepository parallelBranchRepository,
                         DelayTaskRepository delayTaskRepository,
                         UrgeIntervalRepository urgeIntervalRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowBackupRepository = workflowBackupRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.parallelBranchRepository = parallelBranchRepository;
        this.delayTaskRepository = delayTaskRepository;
        this.urgeIntervalRepository = urgeIntervalRepository;
    }


    /**
     * 构建延迟触发执行服务
     *
     * @param task 延迟任务
     * @return 延迟触发执行服务
     */
    public FlowDelayTriggerService createDelayTriggerService(DelayTask task) {
        this.verify();
        return new FlowDelayTriggerService(task,
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository);
    }


    /**
     * 构建流程动作服务
     *
     * @param flowSession 流程会话
     * @return 流程动作服务
     */
    public FlowActionService createFlowActionService(FlowSession flowSession) {
        this.verify();
        return new FlowActionService(flowSession.toActionRequest(),
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository);
    }


    /**
     * 构建流程服务
     *
     * @return 流程服务
     */
    public FlowService createFlowService() {
        this.verify();
        return new FlowService(workflowRepository,
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository);
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

    public IFlowOperator getOperatorById(long id) {
        return flowOperatorGateway.get(id);
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

    public void saveRecord(FlowRecord flowRecord) {
        flowRecordRepository.save(flowRecord);
    }

    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return flowRecordRepository.findCurrentNodeRecords(fromId, nodeId);
    }

    public List<FlowRecord> findProcessRecords(String processId) {
        return flowRecordRepository.findProcessRecords(processId);
    }

    public List<FlowRecord> findAfterRecords(String processId,long currentId) {
        return flowRecordRepository.findAfterRecords(processId,currentId);
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
