package com.codingapi.flow.session;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.TitleGroovyRequest;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
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
    @Setter
    private FlowRecord currentRecord;

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


    /**
     * 获取转交之后的审批人
     *
     * @param currentOperator 当前操作者
     * @return 转交之后的审批人
     */
    public IFlowOperator loadFinalForwardOperator(IFlowOperator currentOperator) {
        if (currentOperator.forwardOperator() != null) {
            return this.loadFinalForwardOperator(currentOperator.forwardOperator());
        }
        return currentOperator;
    }


    /**
     * 构建开始会话
     *
     * @param currentOperator 当前操作者
     * @param workflow        流程设计
     * @param currentNode     当前节点
     * @param currentAction   当前动作
     * @param formData        表单数据
     * @param backupId        流程备份id
     * @return 新的会话
     */
    public static FlowSession startSession(IFlowOperator currentOperator,
                                           Workflow workflow,
                                           IFlowNode currentNode,
                                           IFlowAction currentAction,
                                           FormData formData,
                                           long backupId) {
        return new FlowSession(currentOperator, workflow, currentNode, currentAction, formData, null, new ArrayList<>(), backupId, new FlowAdvice());
    }


    /**
     * 创建流程请求
     */
    public FlowCreateRequest toCreateRequest() {
        FlowCreateRequest request = new FlowCreateRequest();
        IFlowNode startNode = workflow.getStartNode();
        IFlowAction action = startNode.actionManager().getFirstAction();
        request.setWorkId(workflow.getId());
        request.setFormData(formData.toMapData());
        request.setActionId(action.id());
        request.setOperatorId(currentOperator.getUserId());
        return request;
    }


    /**
     * 创建流程动作请求
     */
    public FlowActionRequest toActionRequest() {
        FlowActionRequest request = new FlowActionRequest();
        request.setRecordId(currentRecord.getId());
        request.setFormData(formData.toMapData());
        request.setAdvice(new FlowAdviceBody(this));
        return request;
    }


    /**
     * 获取流程开始节点
     */
    public IFlowNode getStartNode() {
        return workflow.getStartNode();
    }

    /**
     * 获取流程的创建者
     */
    public IFlowOperator getCreatedOperator() {
        return workflow.getCreatedOperator();
    }

    /**
     * 获取流程设计编号
     */
    public String getWorkCode() {
        return workflow.getCode();
    }

    public String getCurrentNodeId() {
        return currentNode.getId();
    }

    public String getCurrentNodeType() {
        return currentNode.getType();
    }

    public String getCurrentNodeName() {
        return currentNode.getName();
    }

    /**
     * 获取下一节点列表
     *
     * @return 下一节点列表
     */
    public List<IFlowNode> matchNextNodes() {
        List<IFlowNode> nodeList = workflow.nextNodes(this.getCurrentNode());
        if (nodeList == null || nodeList.isEmpty()) {
            return nodeList;
        }
        IFlowNode nextNode = nodeList.get(0);
        return nextNode.filterBranches(nodeList, this);
    }

    /**
     * 获取表单数据
     *
     * @param fieldName 字段名称
     * @return 表单数据
     */
    public Object getFormData(String fieldName) {
        return formData.getDataBody().get(fieldName);
    }

    /**
     * 更新会话
     *
     * @param currentNode 当前节点
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowNode currentNode) {
        return new FlowSession(currentOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, backupId, advice);
    }


    /**
     * 更新会话
     *
     * @param currentAction 当前动作
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowAction currentAction) {
        return new FlowSession(currentOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, backupId, advice);
    }

    /**
     * 更新会话
     *
     * @param currentOperator 当前操作者
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowOperator currentOperator) {
        return new FlowSession(currentOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, backupId, advice);
    }

    /**
     * 创建标题请求对象
     * 从当前session构建TitleGroovyRequest
     */
    public TitleGroovyRequest createTitleRequest() {
        TitleGroovyRequest request = new TitleGroovyRequest();

        // 操作人信息
        if (currentOperator != null) {
            request.setOperatorName(currentOperator.getName());
            request.setOperatorId((int) currentOperator.getUserId());
            request.setIsFlowManager(currentOperator.isFlowManager());
        }

        // 流程信息
        if (workflow != null) {
            request.setWorkflowTitle(workflow.getTitle());
            request.setWorkflowCode(workflow.getCode());
        }

        // 节点信息
        if (currentNode != null) {
            request.setNodeName(currentNode.getName());
            request.setNodeType(currentNode.getType());
        }

        // 创建人信息
        if (workflow != null && workflow.getCreatedOperator() != null) {
            request.setCreatorName(workflow.getCreatedOperator().getName());
        }

        // 表单数据
        request.setFormData(formData != null ? formData.toMapData() : null);

        // 流程编号（从record获取）
        if (currentRecord != null) {
            request.setWorkCode(currentRecord.getWorkCode());
        }

        return request;
    }
}
