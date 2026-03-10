package com.codingapi.flow.context;

import com.codingapi.flow.context.service.FlowRecordSaveService;
import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.exception.FlowStateException;
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

/**
 *  流程引擎仓库持有者上下文,负责持有流程引擎相关的仓库实例,并提供相关服务的构建方法
 */
public class RepositoryHolderContext {

    @Getter
    private final static RepositoryHolderContext instance = new RepositoryHolderContext();

    private RepositoryHolderContext() {
    }

    @Getter
    private WorkflowRepository workflowRepository;
    @Getter
    private WorkflowRuntimeRepository workflowRuntimeRepository;
    @Getter
    private FlowRecordRepository flowRecordRepository;
    @Getter
    private FlowTodoRecordRepository flowTodoRecordRepository;
    @Getter
    private FlowTodoMergeRepository flowTodoMergeRepository;
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
                && workflowRuntimeRepository != null
                && flowRecordRepository != null
                && flowTodoRecordRepository != null
                && flowTodoMergeRepository != null
                && flowOperatorGateway != null
                && workflowRepository != null
                && urgeIntervalRepository != null;
    }


    public void verify() {
        if (!isRegistered()) {
            throw FlowStateException.repositoryNotRegistered();
        }
    }

    public void register(WorkflowRepository workflowRepository,
                         WorkflowRuntimeRepository workflowRuntimeRepository,
                         FlowRecordRepository flowRecordRepository,
                         FlowTodoRecordRepository flowTodoRecordRepository,
                         FlowTodoMergeRepository flowTodoMergeRepository,
                         FlowOperatorGateway flowOperatorGateway,
                         ParallelBranchRepository parallelBranchRepository,
                         DelayTaskRepository delayTaskRepository,
                         UrgeIntervalRepository urgeIntervalRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowRuntimeRepository = workflowRuntimeRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.flowTodoRecordRepository = flowTodoRecordRepository;
        this.flowTodoMergeRepository = flowTodoMergeRepository;
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
                workflowRuntimeRepository);
    }


    /**
     * 构建流程动作服务
     *
     * @param flowSession 流程会话
     * @return 流程动作服务
     */
    public FlowActionService createFlowActionService(FlowSession flowSession) {
        this.verify();
        return new FlowActionService(flowSession.toActionRequest());
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
                flowTodoRecordRepository,
                flowTodoMergeRepository,
                workflowRuntimeRepository,
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
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecords);
        flowRecordSaveService.saveAll();
    }

    public void saveRecord(FlowRecord flowRecord) {
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecord);
        flowRecordSaveService.saveAll();
    }

    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return flowRecordRepository.findCurrentNodeRecords(fromId, nodeId);
    }

    public List<FlowRecord> findProcessRecords(String processId) {
        return flowRecordRepository.findProcessRecords(processId);
    }

    public List<FlowRecord> findAfterRecords(String processId, long currentId) {
        return flowRecordRepository.findAfterRecords(processId, currentId);
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
