package com.codingapi.flow.context;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.*;
import java.util.Collections;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowDelayTriggerService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;

import java.util.List;

/**
 *  流程引擎仓库持有者上下文,负责持有流程引擎相关的仓库实例,并提供相关服务的构建方法
 */
public class RepositoryHolderContext implements IRepositoryHolder {

    @Getter
    private final static RepositoryHolderContext instance = new RepositoryHolderContext();

    private RepositoryHolderContext() {
    }

    @Getter
    private WorkflowService workflowService;
    @Getter
    private FlowRecordService flowRecordService;
    @Getter
    private FlowOperatorGateway flowOperatorGateway;
    @Getter
    private ParallelBranchRepository parallelBranchRepository;
    @Getter
    private DelayTaskRepository delayTaskRepository;
    @Getter
    private UrgeIntervalRepository urgeIntervalRepository;
    @Getter
    private FlowOperatorAssignmentRepository flowOperatorAssignmentRepository;

    /**
     * 是否已经注册成功
     */
    public boolean isRegistered() {
        return parallelBranchRepository != null
                && delayTaskRepository != null
                && workflowService != null
                && flowRecordService != null
                && flowOperatorGateway != null
                && urgeIntervalRepository != null
                && flowOperatorAssignmentRepository != null;
    }


    public void verify() {
        if (!isRegistered()) {
            throw FlowStateException.repositoryNotRegistered();
        }
    }

    public void register(WorkflowService workflowService,
                         FlowRecordService flowRecordService,
                         FlowOperatorGateway flowOperatorGateway,
                         ParallelBranchRepository parallelBranchRepository,
                         DelayTaskRepository delayTaskRepository,
                         UrgeIntervalRepository urgeIntervalRepository,
                         FlowOperatorAssignmentRepository flowOperatorAssignmentRepository) {
        this.workflowService = workflowService;
        this.flowRecordService = flowRecordService;
        this.flowOperatorGateway = flowOperatorGateway;
        this.parallelBranchRepository = parallelBranchRepository;
        this.delayTaskRepository = delayTaskRepository;
        this.urgeIntervalRepository = urgeIntervalRepository;
        this.flowOperatorAssignmentRepository = flowOperatorAssignmentRepository;
    }


    /**
     * 构建延迟触发执行服务
     *
     * @param task 延迟任务
     * @return 延迟触发执行服务
     */
    public FlowDelayTriggerService createDelayTriggerService(DelayTask task) {
        this.verify();
        return new FlowDelayTriggerService(task,this);
    }


    /**
     * 构建流程动作服务
     *
     * @param flowSession 流程会话
     * @return 流程动作服务
     */
    public FlowActionService createFlowActionService(FlowSession flowSession) {
        this.verify();
        return new FlowActionService(flowSession.toActionRequest(),this);
    }


    /**
     * 构建流程服务
     *
     * @return 流程服务
     */
    public FlowService createFlowService() {
        this.verify();
        return new FlowService(this);
    }

    public FlowRecord getRecordById(long id) {
        return flowRecordService.getFlowRecord(id);
    }

    public List<IFlowOperator> findOperatorByIds(List<Long> ids) {
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
        flowRecordService.saveFlowRecords(flowRecords);
    }

    public void saveRecord(FlowRecord flowRecord) {
        flowRecordService.saveFlowRecord(flowRecord);
    }

    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return flowRecordService.findFlowRecordCurrentNodeRecords(fromId, nodeId);
    }

    public List<FlowRecord> findProcessRecords(String processId) {
        return flowRecordService.findFlowRecordByProcessId(processId);
    }

    public List<FlowRecord> findAfterRecords(String processId, long currentId) {
        return flowRecordService.findFlowRecordAfterRecords(processId, currentId);
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

    @Override
    public void saveUrgeInterval(UrgeInterval interval) {
        urgeIntervalRepository.save(interval);
    }

    @Override
    public UrgeInterval getLatestUrgeInterval(String processId, long recordId) {
        return urgeIntervalRepository.getLatest(processId, recordId);
    }

    @Override
    public void saveOperatorAssignment(String processId, String nodeId, List<Long> operatorIds) {
        flowOperatorAssignmentRepository.save(processId, nodeId, operatorIds);
    }

    @Override
    public List<Long> findAssignedOperatorIds(String processId, String nodeId) {
        return flowOperatorAssignmentRepository.findOperatorIds(processId, nodeId);
    }
}
