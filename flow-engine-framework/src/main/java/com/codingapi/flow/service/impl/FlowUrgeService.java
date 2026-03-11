package com.codingapi.flow.service.impl;

import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.event.FlowRecordUrgeEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.manager.WorkflowStrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.workflow.UrgeStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;

/**
 * 催办服务
 */
public class FlowUrgeService {

    private final FlowUrgeRequest request;
    private final FlowRecordService flowRecordService;
    private final FlowOperatorGateway flowOperatorGateway;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;

    public FlowUrgeService(FlowUrgeRequest request,IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.flowOperatorGateway = repositoryHolder.getFlowOperatorGateway();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    /**
     * 催办
     */
    public void urge() {
        request.verify();
        // 验证当前用户
        FlowRecord currentRecord = flowRecordService.getFlowRecord(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (currentRecord.isTodo()) {
            throw FlowStateException.recordAlreadyTodo();
        }
        if (currentRecord.isFinish()) {
            throw FlowStateException.recordNotSupportRevoke();
        }

        long currentOperatorId = currentRecord.getCurrentOperatorId();
        if (currentOperatorId != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }

        UrgeInterval urgeInterval = repositoryHolder.getLatestUrgeInterval(currentRecord.getProcessId(), request.getRecordId());
        if (urgeInterval != null) {
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(currentRecord.getWorkRuntimeId());
            Workflow workflow = workflowRuntime.toWorkflow();
            WorkflowStrategyManager strategyManager = workflow.strategyManager();
            if (strategyManager.isEnableUrge()) {
                UrgeStrategy urgeStrategy = strategyManager.getStrategy(UrgeStrategy.class);
                if (!urgeStrategy.hasUrge(urgeInterval)) {
                    throw FlowStateException.recordLimitUrgeError();
                }
            }
        }

        IFlowOperator currentOperator = flowOperatorGateway.get(currentOperatorId);

        List<FlowRecord> todoRecords = flowRecordService.findFlowRecordTodoRecords(currentRecord.getProcessId());
        // 保存催办记录
        repositoryHolder.saveUrgeInterval(new UrgeInterval(currentRecord));

        List<IFlowEvent> flowEvents = new ArrayList<>();

        for (FlowRecord todoRecord : todoRecords) {
            flowEvents.add(new FlowRecordUrgeEvent(todoRecord, currentOperator));
        }

        flowEvents.forEach(EventPusher::push);
    }
}
