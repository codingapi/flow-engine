package com.codingapi.flow.session;

import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
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
     * 当前流程表单
     */
    private final FormMeta formMeta;
    /**
     * 当前流程设计
     */
    private final Workflow workflow;
    /**
     * 当前流程节点
     */
    private final IAuditNode currentNode;

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


    public FlowSession(IFlowOperator currentOperator, FormMeta formMeta, Workflow workflow, IAuditNode currentNode, FormData formData, long backupId) {
        this(currentOperator, formMeta, workflow, currentNode, formData, backupId, FlowAdvice.nullFlowAdvice());
    }

    public FlowSession(IFlowOperator currentOperator, FormMeta formMeta, Workflow workflow, IAuditNode currentNode, FormData formData, long backupId, FlowAdvice advice) {
        this.currentOperator = currentOperator;
        this.formMeta = formMeta;
        this.workflow = workflow;
        this.currentNode = currentNode;
        this.formData = formData;
        this.backupId = backupId;
        this.advice = advice;
    }

    /**
     * 获取流程的创建者
     */
    public IFlowOperator getCreatedOperator() {
        return workflow.getCreatedOperator();
    }

    /**
     * 获取流程的开始节点
     */
    public IFlowNode getStartNode() {
        return workflow.getStartNode();
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

    public List<IAuditNode> nextNodes() {
        return workflow.nextNodes(this);
    }

    public Object getFormData(String fieldName) {
        return formData.getDataBody().get(fieldName);
    }

    public FlowSession updateSession(IAuditNode currentNode) {
        return new FlowSession(currentOperator, formMeta, workflow, currentNode, formData, backupId, advice);
    }

    public FlowSession updateSession(IFlowOperator currentOperator) {
        return new FlowSession(currentOperator, formMeta, workflow, currentNode, formData, backupId, advice);
    }
}
