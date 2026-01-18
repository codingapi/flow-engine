package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.PassAction;
import com.codingapi.flow.backup.WorkflowBackup;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class FlowActionService {

    private final FlowActionRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public void action() {
        request.verify();
        // 验证当前用户
        IFlowOperator currentOperator = flowOperatorGateway.get(request.getAdvice().getOperatorId());
        if (currentOperator == null) {
            throw new IllegalArgumentException("currentOperator not exist");
        }
        FlowRecord flowRecord = flowRecordRepository.get(request.getRecordId());
        if (flowRecord == null) {
            throw new IllegalArgumentException("record not exist");
        }
        if (!flowRecord.isTodo()) {
            throw new IllegalArgumentException("record has done");
        }

        long currentOperatorId = flowRecord.getCurrentOperatorId();
        if (currentOperatorId != currentOperator.getUserId()) {
            throw new IllegalArgumentException("currentOperator not match");
        }

        WorkflowBackup workflowBackup = workflowBackupRepository.get(flowRecord.getWorkBackupId());
        if (workflowBackup == null) {
            throw new IllegalArgumentException("workflow not exist");
        }

        Workflow workflow = workflowBackup.toWorkflow();
        IFlowNode currentNode = workflow.getNode(flowRecord.getNodeId());
        if (currentNode == null) {
            throw new IllegalArgumentException("currentNode not exist");
        }
        IFlowAction flowAction = currentNode.actions().getActionById(request.getAdvice().getActionId());
        if (flowAction == null) {
            throw new IllegalArgumentException("action not exist");
        }

        // 构建表单数据
        FormData formData = new FormData(workflow.getForm());
        formData.reset(request.getFormData());

        List<FlowRecord> currentRecords = flowRecordRepository.findRecordsByFromId(flowRecord.getFromId());
        FlowSession session = new FlowSession(currentOperator, workflow.getForm(), workflow, currentNode, formData, workflowBackup.getId(), request.getAdvice().getAdvice());

        List<IFlowEvent> flowEvents = new ArrayList<>();

        // 判断当前节点是否已经完成
        boolean done = flowAction.isDone(session, flowRecord, currentRecords);
        if (done) {
            List<FlowRecord> records = flowAction.trigger(session, flowRecord);
            if (records == null || records.isEmpty()) {
                throw new IllegalArgumentException("action not return record");
            }
            for (FlowRecord record : records) {
                if (record.isShow()) {
                    flowEvents.add(new FlowRecordTodoEvent(record));
                }
            }
            flowRecord.update(formData.toMapData(), request.getAdvice().getAdvice(), request.getAdvice().getSignKey(), true);
            // 判断是否结束
            if (records.size() == 1) {
                FlowRecord record = records.get(0);
                if (record.isNodeType(EndNode.NODE_TYPE)) {
                    boolean flowFinish = flowAction instanceof PassAction;
                    // 添加当前节点到记录中
                    records.add(flowRecord);
                    // 添加历史记录到记录中
                    List<FlowRecord> historyRecords = flowRecordRepository.findRecordsByProcessId(flowRecord.getProcessId());
                    records.addAll(historyRecords);
                    // 设置状态为完成
                    records.forEach(item -> {
                        item.finish(flowFinish);
                    });

                    // 流程是否正常结束
                    if (flowFinish) {
                        flowEvents.add(new FlowRecordFinishEvent(record));
                    }
                }
                // 添加流程结束事件
                flowEvents.add(new FlowRecordDoneEvent(record));
            }
            flowRecordRepository.saveAll(records);
        } else {
            // 判断是否为串行多操作者
            if (currentNode.strategies().isSequenceMultiOperator()) {
                int nextRecordNodeOrder = flowRecord.getNodeOrder() + 1;
                FlowRecord nextRecord = currentRecords.stream().filter(record -> record.getNodeOrder() == nextRecordNodeOrder).findFirst().orElse(null);
                if (nextRecord != null) {
                    // 展示下一个审批人的待办
                    nextRecord.show();
                    flowEvents.add(new FlowRecordTodoEvent(nextRecord));
                    flowRecordRepository.save(nextRecord);
                }
            }
            flowRecord.update(formData.toMapData(), request.getAdvice().getAdvice(), request.getAdvice().getSignKey(), false);
            flowRecordRepository.save(flowRecord);
        }

        // 推送待办事件
        for (IFlowEvent event : flowEvents) {
            EventPusher.push(event);
        }
    }
}
