package com.codingapi.flow.service.impl;

import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.NodeOperators;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowAdvice;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FlowCreateService {

    private final FlowCreateRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
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
        workflow.matchCreatedOperator(currentOperator);
        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());

        IFlowNode currentNode = workflow.getStartNode();
        FlowSession session = new FlowSession(currentOperator, workflow.getForm(), workflow, currentNode, formData);

        NodeOperators currentOperators = currentNode.operators(session);
        if (!currentOperators.match(currentOperator)) {
            throw new IllegalArgumentException("node operator not match");
        }

        FlowAdvice flowAdvice = null;

    }
}
