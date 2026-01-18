package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.event.FlowRecordStartEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
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

    public void create() {
        request.verify();
        Workflow workflow = workflowRepository.get(request.getWorkId());
        if (workflow == null) {
            throw new IllegalArgumentException("workflow not found");
        }
        workflow.verify();
        // 获取备份
        WorkflowBackup workflowBackup = workflowBackupRepository.getByWorkId(workflow.getId(), workflow.getCreatedTime());
        if (workflowBackup == null) {
            workflowBackup = new WorkflowBackup(workflow);
            workflowBackupRepository.save(workflowBackup);
        }
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getAdvice().getOperatorId());
        if (!workflow.matchCreatedOperator(currentOperator)) {
            throw new IllegalArgumentException("operator not match");
        }
        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());

        IFlowNode currentNode = workflow.getStartNode();
        FlowSession session = new FlowSession(currentOperator, workflow.getForm(), workflow, currentNode, formData, workflowBackup.getId());

        OperatorManager currentOperators = currentNode.operators(session);
        if (!currentOperators.match(currentOperator)) {
            throw new IllegalArgumentException("node operator not match");
        }

        IFlowAction action = currentNode.actions().getActionById(request.getAdvice().getActionId());

        FlowRecord flowRecord = new FlowRecord(session, action.id(), RandomUtils.generateStringId(), 0, 0);
        flowRecord.verify();
        flowRecordRepository.save(flowRecord);

        List<IFlowEvent> events = new ArrayList<>();
        events.add(new FlowRecordStartEvent(flowRecord));
        events.add(new FlowRecordTodoEvent(flowRecord));

        // 推送事件
        for (IFlowEvent event : events) {
            EventPusher.push(event);
        }
    }
}
