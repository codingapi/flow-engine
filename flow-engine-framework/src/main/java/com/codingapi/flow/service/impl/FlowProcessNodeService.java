package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowProcessNodeRequest;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程节点记录服务
 */
public class FlowProcessNodeService {

    private final FlowProcessNodeRequest request;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;

    // 当前操作人
    private final IFlowOperator currentOperator;
    // 当前的流程记录，当id为workId时flowRecord为空
    private FlowRecord flowRecord;
    // 当前的流程设计器
    private Workflow workflow;
    // 流程节点记录
    private final List<ProcessNode> nodeList;


    public FlowProcessNodeService(FlowProcessNodeRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.currentOperator = repositoryHolder.getOperatorById(request.getOperatorId());
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
        this.nodeList = new ArrayList<>();
        this.initData();
    }


    private void initData() {
        String id = this.request.getId();
        if (this.isCreateWorkflow()) {
            this.workflow = workflowService.getWorkflow(id);
        } else {
            this.flowRecord = flowRecordService.getFlowRecord(Long.parseLong(id));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(id));
            }
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
            this.workflow = workflowRuntime.toWorkflow();
        }
    }

    private boolean isCreateWorkflow() {
        String id = this.request.getId();
        return !id.matches("^[0-9]+$");
    }


    public List<ProcessNode> processNodes() {
        // load history data
        if (this.flowRecord != null) {
            this.loadHistoryData();
            if (this.flowRecord.isDone()) {
                // load end node
                this.loadEndNode(this.flowRecord.isFinish());
                return nodeList;
            }
        }
        // load next node data
        this.loadNextData();
        // load end node
        this.loadEndNode(false);
        return nodeList;
    }


    private void loadHistoryData() {
        List<FlowRecord> allRecords = flowRecordService.findFlowRecordByProcessId(this.flowRecord.getProcessId());
        Map<String, ProcessNode> nodeMap = new LinkedHashMap<>();
        for (FlowRecord record : allRecords) {
            ProcessNode existing = nodeMap.get(record.getNodeId());
            if (existing != null) {
                existing.addFlowRecordOperator(record);
                existing.resetApproveState(record);
            } else {
                nodeMap.put(record.getNodeId(), ProcessNode.createByRecord(record, this.workflow));
            }
        }
        this.nodeList.addAll(nodeMap.values());
    }

    private void loadEndNode(boolean finish) {
        IFlowNode endNode = this.workflow.getEndNode();
        this.nodeList.add(ProcessNode.createByEndNode(endNode, finish));
    }


    private void loadNextData() {
        if (this.flowRecord == null) {
            IFlowNode currentNode = this.workflow.getStartNode();
            FlowSession flowSession = this.buildFlowSession(currentNode, currentOperator, currentOperator, currentOperator, 0);
            this.addFlowNode(currentNode, flowSession);
            this.fetchFlowNode(flowSession);
        } else {
            List<FlowRecord> todoRecords = this.flowRecordService.findFlowRecordTodoRecords(flowRecord.getProcessId());
            if (todoRecords != null && !todoRecords.isEmpty()) {
                for (FlowRecord todoRecord : todoRecords) {
                    IFlowNode currentNode = this.workflow.getFlowNode(todoRecord.getNodeId());
                    IFlowOperator createOperator = this.repositoryHolder.getOperatorById(todoRecord.getCreateOperatorId());
                    IFlowOperator submitOperator = this.repositoryHolder.getOperatorById(todoRecord.getSubmitOperatorId());

                    FlowSession flowSession = this.buildFlowSession(currentNode, currentOperator, createOperator, submitOperator, todoRecord.getWorkRuntimeId());
                    this.fetchFlowNode(flowSession);
                }
            }
        }
    }


    private FlowSession buildFlowSession(IFlowNode currentNode,
                                         IFlowOperator currentOperator,
                                         IFlowOperator createdOperator,
                                         IFlowOperator submitOperator,
                                         long backupId) {
        ActionManager actionManager = currentNode.actionManager();
        IFlowAction flowAction = actionManager.getAction(PassAction.class);
        FormData formData = new FormData(this.workflow.getForm());
        formData.reset(this.request.getFormData());

        return new FlowSession(
                this.repositoryHolder,
                currentOperator,
                createdOperator,
                submitOperator,
                this.workflow,
                currentNode,
                flowAction,
                formData,
                this.flowRecord,
                new ArrayList<>(),
                backupId,
                FlowAdvice.nullFlowAdvice()
        );
    }


    private void fetchFlowNode(FlowSession flowSession) {
        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        if (nextNodes != null && !nextNodes.isEmpty()) {
            for (IFlowNode flowNode : nextNodes) {
                this.addFlowNode(flowNode, flowSession);

                FlowSession nextSession = flowSession.updateSession(flowNode);
                this.fetchFlowNode(nextSession);
            }
        }

    }

    private void addFlowNode(IFlowNode flowNode, FlowSession flowSession) {
        // 仅添加展示节点，且非结束节点
        if (flowNode instanceof IDisplayNode) {
            if (!(flowNode instanceof EndNode)) {

                if (flowNode instanceof StartNode) {
                    List<IFlowOperator> operators = new ArrayList<>();
                    operators.add(this.currentOperator);
                    this.nodeList.add(ProcessNode.createByNode(flowNode, OperatorSelectType.SCRIPT, operators));
                } else {
                    OperatorLoadStrategy operatorLoadStrategy = flowNode.strategyManager().getStrategy(OperatorLoadStrategy.class);
                    OperatorSelectType operatorSelectType = operatorLoadStrategy.getSelectType();

                    OperatorManager operatorManager = flowNode.strategyManager().loadOperators(flowSession);
                    List<IFlowOperator> operators = operatorManager.getOperators();

                    this.nodeList.add(ProcessNode.createByNode(flowNode, operatorSelectType, operators));
                }
            }
        }
    }

}
