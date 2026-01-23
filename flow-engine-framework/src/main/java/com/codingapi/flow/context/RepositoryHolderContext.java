package com.codingapi.flow.context;

import com.codingapi.flow.delay.DelayTask;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
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

    /**
     * 是否已经注册成功
     */
    public boolean isRegistered() {
        return parallelBranchRepository != null
                && delayTaskRepository != null
                && workflowBackupRepository != null
                && flowRecordRepository != null
                && flowOperatorGateway != null
                && workflowRepository != null;
    }


    public void verify(){
        if(!isRegistered()){
            throw new FlowConfigException(FlowConfigException.ERROR_CODE_PREFIX + "DELAY_TASK_NOT_REGISTER");
        }
    }

    public void register(WorkflowRepository workflowRepository,
                         WorkflowBackupRepository workflowBackupRepository,
                         FlowRecordRepository flowRecordRepository,
                         FlowOperatorGateway flowOperatorGateway,
                         ParallelBranchRepository parallelBranchRepository,
                         DelayTaskRepository delayTaskRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowBackupRepository = workflowBackupRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.parallelBranchRepository = parallelBranchRepository;
        this.delayTaskRepository = delayTaskRepository;
    }


    /**
     * 构建延迟触发执行服务
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
     * 构建流程服务
     * @return 流程服务
     */
    public FlowService createFlowService() {
        this.verify();
        return new FlowService(workflowRepository,
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository,
                parallelBranchRepository,
                delayTaskRepository);
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

    public void saveRecord(FlowRecord flowRecord){
        flowRecordRepository.save(flowRecord);
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
