package com.codingapi.flow.session;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;

import java.util.List;

/**
 * 流程会话对象
 */
@Getter
public class FlowSession {

    /**
     * 当前操作者
     */
    private final IFlowOperator currentOperator;
    /**
     * 当前流程设计
     */
    private final Workflow workflow;
    /**
     * 当前流程节点
     */
    private final IFlowNode currentNode;

    /**
     * 当前流程动作
     */
    private final IFlowAction currentAction;

    /**
     * 当前审批流程记录
     */
    private final FlowRecord currentRecord;

    /**
     * 当前节点的流程记录
     */
    private final List<FlowRecord> currentNodeRecords;

    /**
     * 当前流程表单数据
     */
    private final FormData formData;
    /**
     * 流程备份id
     */
    private final long backupId;

    /**
     * 审批意见
     */
    private final FlowAdvice advice;


    public FlowSession(IFlowOperator currentOperator,
                       Workflow workflow,
                       IFlowNode currentNode,
                       IFlowAction currentAction,
                       FormData formData,
                       FlowRecord currentRecord,
                       List<FlowRecord> currentNodeRecords,
                       long backupId,
                       FlowAdvice advice) {
        this.currentOperator = currentOperator;
        this.workflow = workflow;
        this.currentAction = currentAction;
        this.currentNode = currentNode;
        this.currentRecord = currentRecord;
        this.currentNodeRecords = currentNodeRecords;
        this.formData = formData;
        this.backupId = backupId;
        this.advice = advice;
    }


    public static FlowSession startSession(IFlowOperator currentOperator,
                                      Workflow workflow,
                                      IFlowNode currentNode,
                                      FormData formData,
                                      long backupId) {
        return new FlowSession(currentOperator, workflow, currentNode, null,formData, null,null, backupId, new FlowAdvice());
    }


    /**
     * 获取流程的创建者
     */
    public IFlowOperator getCreatedOperator() {
        return workflow.getCreatedOperator();
    }

    public String getWorkCode() {
        return workflow.getCode();
    }

    public String getWorkTitle() {
        return workflow.getTitle();
    }

    public String getCurrentNodeId() {
        return currentNode.getId();
    }

    public String getCurrentNodeType() {
        return currentNode.getType();
    }

    public List<IFlowNode> nextNodes() {
        return workflow.nextNodes(this.getCurrentNode());
    }

    public Object getFormData(String fieldName) {
        return formData.getDataBody().get(fieldName);
    }

    public FlowSession updateSession(IFlowNode currentNode) {
        return new FlowSession(currentOperator, workflow, currentNode,currentAction, formData,currentRecord, currentNodeRecords, backupId, advice);
    }

    public FlowSession updateSession(IFlowOperator currentOperator) {
        return new FlowSession(currentOperator, workflow, currentNode,currentAction, formData,currentRecord, currentNodeRecords, backupId, advice);
    }
}
