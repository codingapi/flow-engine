package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.IFlowNode;
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
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程节点记录服务
 */
public class FlowProcessNodeService {

    private final FlowProcessNodeRequest request;
    private final IFlowOperator currentOperator;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

    private final IRepositoryHolder repositoryHolder;

    // 当前的流程记录，当id为workId时flowRecord为空
    private FlowRecord flowRecord;
    // 当前的流程设计器
    private Workflow workflow;
    // 当前的节点
    private IFlowNode currentNode;
    // 流程节点记录
    private final List<ProcessNode> nodeList;


    public FlowProcessNodeService(FlowProcessNodeRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.currentOperator = repositoryHolder.getOperatorById(request.getOperatorId());
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
        this.nodeList = new ArrayList<>();
        this.loadWorkflow();
    }


    private void loadWorkflow() {
        String id = this.request.getId();
        if (this.request.isCreateWorkflow()) {
            this.workflow = workflowService.getWorkflow(id);
            this.currentNode = this.workflow.getStartNode();
        } else {
            FlowRecord flowRecord = flowRecordService.getFlowRecord(Long.parseLong(id));
            if (flowRecord == null) {
                throw FlowNotFoundException.record(Long.parseLong(id));
            }
            this.flowRecord = flowRecord;
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(flowRecord.getWorkRuntimeId());
            if (workflowRuntime == null) {
                throw FlowNotFoundException.workflow(flowRecord.getWorkRuntimeId() + " not found");
            }
            this.workflow = workflowRuntime.toWorkflow();
            this.currentNode = this.workflow.getFlowNode(flowRecord.getNodeId());
        }
    }

    public List<ProcessNode> processNodes() {
        long backupId = 0;
        if (this.flowRecord != null) {
            backupId = this.flowRecord.getWorkRuntimeId();
            // 如果当前记录已结束，则不查询后续流程
            if(this.flowRecord.isDone()){
                List<FlowRecord> historyRecords =  flowRecordService.findFlowRecordByProcessId(this.flowRecord.getProcessId());
                for (FlowRecord historyRecord : historyRecords) {
                    ProcessNode processNode = new ProcessNode(historyRecord, this.workflow);
                    nodeList.add(processNode);
                }
                if(this.flowRecord.isFinish()){
                    nodeList.add(ProcessNode.createEndNode(this.workflow));
                }else {
                    this.loadNextNode(backupId);
                }
                return this.nodeList;
            }else {
                // 查询历史记录
                List<FlowRecord> historyRecords = flowRecordService.findFlowRecordBeforeRecords(flowRecord.getProcessId(), flowRecord.getId());
                for (FlowRecord historyRecord : historyRecords) {
                    ProcessNode processNode = new ProcessNode(historyRecord, this.workflow);
                    nodeList.add(processNode);
                }
            }
        }

        this.loadNextNode(backupId);

        return this.nodeList;
    }


    private void loadNextNode(long backupId){

        ActionManager actionManager = currentNode.actionManager();
        IFlowAction flowAction = actionManager.getAction(PassAction.class);
        FormData formData = new FormData(this.workflow.getForm());
        formData.reset(this.request.getFormData());

        FlowSession flowSession = new FlowSession(
                this.repositoryHolder,
                this.currentOperator,
                this.workflow,
                this.currentNode,
                flowAction,
                formData,
                this.flowRecord,
                new ArrayList<>(),
                backupId,
                FlowAdvice.nullFlowAdvice()
        );

        NextNodeLoader nextNodeLoader = new NextNodeLoader(this.currentNode);
        List<ProcessNode> nextNodes =  nextNodeLoader.loadNextNode(flowSession);

        this.nodeList.addAll(nextNodes);
    }


    private class NextNodeLoader {

        @Getter
        private final List<ProcessNode> nodeList;
        private final IFlowNode currentNode;

        public NextNodeLoader(IFlowNode currentNode) {
            this.currentNode = currentNode;
            this.nodeList = new ArrayList<>();
        }

        private void fetchNextNode(FlowSession flowSession, List<IFlowNode> nexNodes) {
            for (IFlowNode flowNode : nexNodes) {
                List<IFlowOperator> operators = null;
                if (flowNode.getType().equals(StartNode.NODE_TYPE)) {
                    operators = List.of(flowSession.getCurrentOperator());
                } else {
                    NodeStrategyManager nodeStrategyManager = flowNode.strategyManager();
                    OperatorManager operatorManager = nodeStrategyManager.loadOperators(flowSession);
                    operators = operatorManager.getOperators();
                }
                ProcessNode processNode = new ProcessNode(flowNode, operators);
                if (processNode.isFlowNode(this.currentNode)) {
                    processNode.setCurrentState();
                }
                this.nodeList.add(processNode);
                List<IFlowNode> nextNodes = workflow.nextNodes(flowNode);
                this.fetchNextNode(flowSession.updateSession(flowNode), nextNodes);
            }
        }

        public List<ProcessNode> loadNextNode(FlowSession flowSession) {
            this.fetchNextNode(flowSession,List.of(this.currentNode));

            List<ProcessNode> displayNodes = nodeList.stream().filter(ProcessNode::isDisplay).toList();
            List<ProcessNode> processNodeList = new ArrayList<>();
            for (ProcessNode node:displayNodes){
                if(!processNodeList.contains(node)){
                    processNodeList.add(node);
                }
            }
            return processNodeList;
        }
    }


}
