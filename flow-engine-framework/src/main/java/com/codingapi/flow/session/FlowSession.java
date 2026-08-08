package com.codingapi.flow.session;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.domain.SubProcessContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.form.FormDataVerify;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程会话对象
 */
@Getter
public class FlowSession {

    /**
     * 资源持有者
     */
    private final IRepositoryHolder repositoryHolder;

    /**
     * 流程创建者
     */
    @Getter
    private final IFlowOperator createdOperator;

    /**
     * 流程提交人
     */
    @Getter
    private final IFlowOperator submitOperator;

    /**
     * 当前审批者
     */
    @Getter
    private final IFlowOperator currentOperator;
    /**
     * 当前流程设计
     */
    @Getter
    private final Workflow workflow;
    /**
     * 当前流程节点
     */
    @Getter
    private final IFlowNode currentNode;

    /**
     * 当前流程动作
     */
    @Getter
    private final IFlowAction currentAction;

    /**
     * 当前审批流程记录
     */
    @Setter
    private FlowRecord currentRecord;

    /**
     * 当前节点的流程记录
     */
    @Getter
    private final List<FlowRecord> currentNodeRecords;

    /**
     * 当前流程表单数据
     */
    @Getter
    private final FormData formData;
    /**
     * 流程备份id
     */
    @Getter
    private final long workflowRuntimeId;

    /**
     * 审批意见
     */
    @Getter
    private final FlowAdvice advice;

    /**
     * 子流程结果判定上下文，仅在子流程完成回调中存在。
     */
    @Setter
    private SubProcessContext subProcessContext;


    public FlowSession(IRepositoryHolder repositoryHolder,
                       IFlowOperator currentOperator,
                       IFlowOperator createdOperator,
                       IFlowOperator submitOperator,
                       Workflow workflow,
                       IFlowNode currentNode,
                       IFlowAction currentAction,
                       FormData formData,
                       FlowRecord currentRecord,
                       List<FlowRecord> currentNodeRecords,
                       long workflowRuntimeId,
                       FlowAdvice advice) {
        this.repositoryHolder = repositoryHolder;
        this.currentOperator = currentOperator;
        this.workflow = workflow;
        this.currentAction = currentAction;
        this.currentNode = currentNode;
        this.currentRecord = currentRecord;
        this.currentNodeRecords = currentNodeRecords;
        this.formData = formData;
        this.workflowRuntimeId = workflowRuntimeId;
        this.advice = advice;
        this.createdOperator = createdOperator;
        this.submitOperator = submitOperator;
    }


    /**
     * 是否是mock
     */
    public boolean isMock() {
        return this.repositoryHolder instanceof MockRepositoryHolder;
    }

    /**
     * 获取上一节点的审批人员Id列表。
     *
     * <p>以当前流程记录所在节点作为上一节点，按 fromId 查询该节点的流程记录，
     * 提取<b>实际审批过</b>的人员Id并去重、排序后返回。
     *
     * <p>多人审批（或签/并签）场景下，节点完成后其他候选人的待办会被自动置为已办
     * （自动跳过，{@link FlowRecord#isAutoDone()}），但他们并未实际审批，取数时排除，
     * 确保下游节点取到的是真实审批人（issue #188）。
     *
     * <p>仅当该节点为当前节点的直接前驱节点时才返回数据；流程预览等场景下
     * 当前记录可能尚未流转到当前节点（如停留在更早的节点），此时返回空列表，避免误读。
     *
     * @return 上一节点的审批人员Id列表，无法确定时返回空列表
     */
    public List<Long> loadPreviousNodeOperatorIds() {
        if (currentRecord == null || currentNode == null) {
            return List.of();
        }
        IFlowNode previousNode = workflow.getFlowNode(currentRecord.getNodeId());
        if (previousNode == null) {
            return List.of();
        }
        List<IFlowNode> nextNodes = workflow.nextNodes(previousNode);
        boolean directPrevious = nextNodes != null
                && nextNodes.stream().anyMatch(node -> node.getId().equals(currentNode.getId()));
        if (!directPrevious) {
            return List.of();
        }
        return repositoryHolder.findCurrentNodeRecords(currentRecord.getFromId(), currentRecord.getNodeId())
                .stream()
                .filter(record -> !record.isAutoDone())
                .map(FlowRecord::getCurrentOperatorId)
                .distinct()
                .sorted()
                .toList();
    }


    /**
     * 获取转交之后的审批人
     *
     * @param currentOperator 当前操作者
     * @return 转交之后的审批人
     */
    public IFlowOperator loadFinalForwardOperator(IFlowOperator currentOperator) {
        // 传递更新后的 session，确保 forwardOperator(FlowSession) 中的 currentOperator 是当前操作者
        GroovyScriptRequest request = new GroovyScriptRequest(this.updateSession(currentOperator));
        IFlowOperator forward = currentOperator.forwardOperator(request);
        if (forward != null) {
            return this.loadFinalForwardOperator(forward);
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
    public static FlowSession startSession(
            IRepositoryHolder repositoryHolder,
            IFlowOperator currentOperator,
            Workflow workflow,
            IFlowNode currentNode,
            IFlowAction currentAction,
            FormData formData,
            long backupId) {
        return new FlowSession(repositoryHolder, currentOperator, currentOperator, currentOperator, workflow, currentNode, currentAction, formData, null, new ArrayList<>(), backupId, new FlowAdvice());
    }


    /**
     * 创建流程请求，用于自流程的创建
     */
    public FlowCreateRequest toCreateRequest() {
        IFlowNode startNode = workflow.getStartNode();
        IFlowAction action = startNode.actionManager().getActionByType(ActionType.SAVE.name());
        return this.toCreateRequest(workflow.getCode(), currentOperator.getUserId(), action.id(), formData.toMapData());
    }


    /**
     * 创建流程请求，用于自流程的创建
     *
     * @param workCode 流程Code
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    public FlowCreateRequest toCreateRequest(String workCode,
                                             long operatorId,
                                             String actionId,
                                             String formData) {

        FlowCreateRequest request = new FlowCreateRequest();
        request.setActionId(actionId);
        request.setWorkCode(workCode);
        request.setOperatorId(operatorId);
        request.setFormData(JSONObject.parseObject(formData));
        return request;
    }

    /**
     * 创建流程请求，用于自流程的创建（支持手动传入流程标题）
     *
     * @param workCode 流程Code
     * @param actionId 动作类型
     * @param formData 流程数据
     * @param workTitle 手动传入的流程标题；为空时回落流程定义标题
     */
    public FlowCreateRequest toCreateRequest(String workCode,
                                             long operatorId,
                                             String actionId,
                                             String formData,
                                             String workTitle) {

        FlowCreateRequest request = this.toCreateRequest(workCode, operatorId, actionId, formData);
        request.setWorkTitle(workTitle);
        return request;
    }

    /**
     * 创建流程请求，用于自流程的创建
     *
     * @param workCode   流程Code
     * @param actionId 动作类型
     * @param formData 流程数据
     */
    public FlowCreateRequest toCreateRequest(String workCode,
                                             long operatorId,
                                             String actionId,
                                             Map<String, Object> formData) {

        FlowCreateRequest request = new FlowCreateRequest();
        request.setActionId(actionId);
        request.setWorkCode(workCode);
        request.setOperatorId(operatorId);
        request.setFormData(formData);
        return request;
    }

    /**
     * 创建流程请求，用于自流程的创建（支持手动传入流程标题）
     *
     * @param workCode 流程Code
     * @param actionId 动作类型
     * @param formData 流程数据
     * @param workTitle 手动传入的流程标题；为空时回落流程定义标题
     */
    public FlowCreateRequest toCreateRequest(String workCode,
                                             long operatorId,
                                             String actionId,
                                             Map<String, Object> formData,
                                             String workTitle) {

        FlowCreateRequest request = this.toCreateRequest(workCode, operatorId, actionId, formData);
        request.setWorkTitle(workTitle);
        return request;
    }


    /**
     * 创建流程动作请求，用于自定义脚本的执行
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
     * 获取流程的提交者Id
     */
    public long getSubmitOperatorId() {
        if (this.submitOperator != null) {
            return this.submitOperator.getUserId();
        }
        return 0;
    }

    /**
     * 获取流程的提交者名称
     */
    public String getSubmitOperatorName() {
        if (this.submitOperator != null) {
            return this.submitOperator.getName();
        }
        return null;
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
     * @param fieldCode 字段名称
     * @return 表单数据
     */
    public Object getFormData(String fieldCode) {
        return formData.getDataBody().get(fieldCode);
    }

    /**
     * 更新会话
     *
     * @param currentNode 当前节点
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowNode currentNode) {
        return this.copySubProcessContext(new FlowSession(repositoryHolder, currentOperator, createdOperator, submitOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, workflowRuntimeId, advice));
    }


    /**
     * 更新会话
     *
     * @param currentAction 当前动作
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowAction currentAction) {
        return this.copySubProcessContext(new FlowSession(repositoryHolder, currentOperator, createdOperator, submitOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, workflowRuntimeId, advice));
    }

    /**
     * 更新会话
     *
     * @param currentOperator 当前操作者
     * @return 新的会话
     */
    public FlowSession updateSession(IFlowOperator currentOperator) {
        return this.copySubProcessContext(new FlowSession(repositoryHolder, currentOperator, createdOperator, submitOperator, workflow, currentNode, currentAction, formData, currentRecord, currentNodeRecords, workflowRuntimeId, advice));
    }

    private FlowSession copySubProcessContext(FlowSession session) {
        session.setSubProcessContext(this.subProcessContext);
        return session;
    }

    /**
     * 查询当前主流程中指定子流程节点已经结束的最终记录。
     *
     * @param subProcessNodeId 子流程节点 ID
     * @return 每个已结束子流程实例的最终业务记录
     */
    public List<FlowRecord> findSubProcessRecords(String subProcessNodeId) {
        if (currentRecord == null) {
            return List.of();
        }
        List<SubProcessRecord> records;
        if (subProcessContext != null
                && subProcessContext.getSubProcessRecord().getNodeId().equals(subProcessNodeId)) {
            records = List.of(subProcessContext.getSubProcessRecord());
        } else {
            records = repositoryHolder.getSubProcessRepository()
                    .findByParentProcessIdAndNodeId(currentRecord.getProcessId(), subProcessNodeId);
        }
        List<Long> ids = records.stream()
                .flatMap(record -> record.findFinishedRecordIds().stream())
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return repositoryHolder.getFlowRecordService().findFlowRecordByIds(ids).stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 获取本次子流程节点创建的实例总数。
     */
    public int getSubProcessTotal() {
        return subProcessContext == null ? 0 : subProcessContext.getSubProcessRecord().getTotalCount();
    }

    /**
     * 获取本次刚结束的子流程最终记录。
     */
    public FlowRecord getCurrentSubProcessRecord() {
        return subProcessContext == null ? null : subProcessContext.getCurrentSubProcessRecord();
    }

    /**
     * 获取节点
     *
     * @param nodeId 节点id
     */
    public IFlowNode getNode(String nodeId) {
        return this.workflow.getFlowNode(nodeId);
    }


    /**
     * 校验表单字段
     */
    public void verifyFormData() {
        List<FormFieldPermission> permissions = this.currentNode.strategyManager().getFieldPermissions();
        FormDataVerify verify = new FormDataVerify(formData, permissions);
        verify.verify();
    }
}
