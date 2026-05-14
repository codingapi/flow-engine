package com.codingapi.flow.service.impl;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ConditionBranchNode;
import com.codingapi.flow.node.nodes.ConditionElseBranchNode;
import com.codingapi.flow.node.nodes.InclusiveBranchNode;
import com.codingapi.flow.node.nodes.InclusiveElseBranchNode;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程节点记录服务
 */
public class FlowProcessNodeService {

    private final FlowProcessNodeRequest request;
    private final IFlowOperator currentOperator;
    private IFlowOperator createdOperator;
    private IFlowOperator submitOperator;
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
            this.createdOperator = this.currentOperator;
            this.submitOperator = null;
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
            this.createdOperator = this.repositoryHolder.getOperatorById(this.flowRecord.getCreateOperatorId());
            this.submitOperator =  this.repositoryHolder.getOperatorById(this.flowRecord.getSubmitOperatorId());
        }
    }

    public List<ProcessNode> processNodes() {
        long backupId = 0;
        if (this.flowRecord != null) {
            backupId = this.flowRecord.getWorkRuntimeId();
            List<FlowRecord> allRecords = flowRecordService.findFlowRecordByProcessId(this.flowRecord.getProcessId());

            if (this.flowRecord.isDone()) {
                // 已完成记录：构建历史节点（会签/多人审批按 nodeId 合并）
                List<FlowRecord> doneRecords = allRecords.stream().filter(FlowRecord::isDone).toList();
                nodeList.addAll(buildHistoryNodes(doneRecords));
                if (this.flowRecord.isFinish()) {
                    nodeList.add(ProcessNode.createEndNode(this.workflow));
                    return this.nodeList;
                }
            } else {
                // 进行中：构建历史节点 + 当前节点
                List<FlowRecord> doneRecords = allRecords.stream().filter(FlowRecord::isDone).toList();
                nodeList.addAll(buildHistoryNodes(doneRecords));

                // 当前节点待办记录 → 当前节点（会签/多人审批合并）
                List<FlowRecord> currentTodoRecords = allRecords.stream()
                        .filter(r -> r.getNodeId().equals(this.currentNode.getId()) && r.isTodo())
                        .toList();
                if (!currentTodoRecords.isEmpty()) {
                    ProcessNode currentProcessNode = nodeList.stream()
                            .filter(n -> n.getNodeId().equals(this.currentNode.getId()))
                            .findFirst()
                            .orElse(null);
                    if (currentProcessNode != null) {
                        // 部分会签已完成：追加待办审批人
                        for (FlowRecord record : currentTodoRecords) {
                            currentProcessNode.addOperator(new ProcessNode.FlowOperatorBody(record));
                        }
                    } else {
                        // 从审批记录创建当前节点
                        currentProcessNode = new ProcessNode(currentTodoRecords.get(0), this.workflow);
                        for (int i = 1; i < currentTodoRecords.size(); i++) {
                            currentProcessNode.addOperator(new ProcessNode.FlowOperatorBody(currentTodoRecords.get(i)));
                        }
                        nodeList.add(currentProcessNode);
                    }
                    currentProcessNode.setState(ProcessNode.STATE_CURRENT);
                }
            }
        }

        this.loadNextNode(backupId);
        deduplicateNodeList();

        return this.nodeList;
    }

    private List<ProcessNode> buildHistoryNodes(List<FlowRecord> records) {
        Map<String, ProcessNode> nodeMap = new LinkedHashMap<>();
        for (FlowRecord record : records) {
            ProcessNode existing = nodeMap.get(record.getNodeId());
            if (existing != null) {
                existing.addOperator(new ProcessNode.FlowOperatorBody(record));
            } else {
                nodeMap.put(record.getNodeId(), new ProcessNode(record, this.workflow));
            }
        }
        return new ArrayList<>(nodeMap.values());
    }

    private void deduplicateNodeList() {
        Set<String> seen = new HashSet<>();
        List<ProcessNode> result = new ArrayList<>();
        for (ProcessNode node : nodeList) {
            if (seen.add(node.getNodeId())) {
                result.add(node);
            }
        }
        this.nodeList.clear();
        this.nodeList.addAll(result);
    }


    private void loadNextNode(long backupId) {

        ActionManager actionManager = currentNode.actionManager();
        IFlowAction flowAction = actionManager.getAction(PassAction.class);
        FormData formData = new FormData(this.workflow.getForm());
        formData.reset(this.request.getFormData());

        FlowSession flowSession = new FlowSession(
                this.repositoryHolder,
                this.currentOperator,
                this.createdOperator,
                this.submitOperator,
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
        List<ProcessNode> nextNodes = nextNodeLoader.loadNextNode(flowSession);

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
                ActionManager actionManager = flowNode.actionManager();
                IFlowAction passAction =  actionManager.getAction(PassAction.class);
                flowSession = flowSession.updateSession(passAction);

                List<IFlowOperator> operators = null;
                if (flowNode.getType().equals(StartNode.NODE_TYPE)) {
                    operators = List.of(flowSession.getCurrentOperator());
                } else {
                    NodeStrategyManager nodeStrategyManager = flowNode.strategyManager();
                    OperatorManager operatorManager = nodeStrategyManager.loadOperators(flowSession.updateSession(flowNode));
                    operators = operatorManager.getOperators();
                }
                ProcessNode processNode = new ProcessNode(flowNode, operators);
                if (processNode.isFlowNode(this.currentNode)) {
                    processNode.setCurrentState();
                }
                this.nodeList.add(processNode);
                List<IFlowNode> nextNodes = workflow.nextNodes(flowNode);
                List<IFlowNode> selected = selectBranchNodes(nextNodes, flowSession.updateSession(flowNode));
                this.fetchNextNode(flowSession.updateSession(flowNode), selected);
            }
        }

        private List<IFlowNode> selectBranchNodes(List<IFlowNode> nextNodes, FlowSession flowSession) {
            if (nextNodes == null || nextNodes.isEmpty()) {
                return nextNodes;
            }
            IFlowNode first = nextNodes.get(0);
            String type = first.getType();
            boolean isBranchControl =
                    ConditionBranchNode.NODE_TYPE.equals(type)
                            || ConditionElseBranchNode.NODE_TYPE.equals(type)
                            || InclusiveBranchNode.NODE_TYPE.equals(type)
                            || InclusiveElseBranchNode.NODE_TYPE.equals(type);
            if (!isBranchControl) {
                return nextNodes;
            }
            try {
                // 优先按 if 条件脚本匹配；脚本天然 fallback else（else.handle() 恒为 true）
                List<IFlowNode> matched = first.filterBranches(nextNodes, flowSession);
                if (matched != null && !matched.isEmpty()) {
                    return matched;
                }
            } catch (Exception ignored) {
                // 表单数据缺失导致条件脚本异常 —— 降级到 else
            }
            List<IFlowNode> elseOnly = nextNodes.stream()
                    .filter(n -> ConditionElseBranchNode.NODE_TYPE.equals(n.getType())
                            || InclusiveElseBranchNode.NODE_TYPE.equals(n.getType()))
                    .toList();
            return elseOnly.isEmpty() ? nextNodes : elseOnly;
        }

        public List<ProcessNode> loadNextNode(FlowSession flowSession) {
            this.fetchNextNode(flowSession, List.of(this.currentNode));

            List<ProcessNode> displayNodes = nodeList.stream().filter(ProcessNode::isDisplay).toList();
            List<ProcessNode> processNodeList = new ArrayList<>();
            for (ProcessNode node : displayNodes) {
                if (!processNodeList.contains(node)) {
                    processNodeList.add(node);
                }
            }
            return sortByWorkflowNodeOrder(processNodeList);
        }

        private List<ProcessNode> sortByWorkflowNodeOrder(List<ProcessNode> processNodes) {
            Map<String, Integer> indexMap = new HashMap<>();
            int[] counter = {0};
            buildNodeIndexMap(workflow.getNodes(), indexMap, counter);
            return processNodes.stream()
                    .sorted(Comparator.comparingInt(
                            n -> indexMap.getOrDefault(n.getNodeId(), Integer.MAX_VALUE)))
                    .toList();
        }

        private void buildNodeIndexMap(List<IFlowNode> nodes, Map<String, Integer> indexMap, int[] counter) {
            if (nodes == null || nodes.isEmpty()) {
                return;
            }
            for (IFlowNode node : nodes) {
                indexMap.put(node.getId(), counter[0]++);
                List<IFlowNode> blocks = node.blocks();
                if (blocks != null && !blocks.isEmpty()) {
                    buildNodeIndexMap(blocks, indexMap, counter);
                }
            }
        }
    }


}
