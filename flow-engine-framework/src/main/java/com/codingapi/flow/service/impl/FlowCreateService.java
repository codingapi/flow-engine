package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.event.FlowRecordStartEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class FlowCreateService {

    private final FlowCreateRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public long create() {
        request.verify();
        Workflow workflow = workflowRepository.get(request.getWorkId());
        if (workflow == null) {
            throw FlowNotFoundException.workflow(request.getWorkId());
        }
        workflow.verify();
        // 获取备份
        WorkflowBackup workflowBackup = workflowBackupRepository.getByWorkId(workflow.getId(), workflow.getCreatedTime());
        if (workflowBackup == null) {
            workflowBackup = new WorkflowBackup(workflow);
            workflowBackupRepository.save(workflowBackup);
        }
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getOperatorId());
        if (!workflow.matchCreatedOperator(currentOperator)) {
            throw FlowPermissionException.accessDenied("create workflow");
        }
        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());

        StartNode currentNode = (StartNode) workflow.getStartNode();
        IFlowAction action = currentNode.actionManager().getActionById(request.getActionId());
        FlowSession session = FlowSession.startSession(currentOperator, workflow, currentNode, action, formData, workflowBackup.getId());

        currentNode.verifySession(session);

        List<FlowRecord> flowRecords = currentNode.generateCurrentRecords(session);

        if (flowRecords.size() > 1) {
            throw new FlowExecutionException("create record error,record size must be 1.");
        }

        flowRecordRepository.saveAll(flowRecords);

        List<IFlowEvent> events = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            events.add(new FlowRecordStartEvent(flowRecord));
            events.add(new FlowRecordTodoEvent(flowRecord));
        }

        // 推送事件
        for (IFlowEvent event : events) {
            EventPusher.push(event);
        }

        FlowRecord currentRecord = flowRecords.get(0);
        return currentRecord.getId();
    }
}
