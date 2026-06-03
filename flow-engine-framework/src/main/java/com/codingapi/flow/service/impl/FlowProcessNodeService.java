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

import java.util.*;
import java.util.function.Consumer;

/**
 * 流程节点记录服务
 */
public class FlowProcessNodeService {

    private final FlowProcessNodeRequest request;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;


    // 当前的流程记录，当id为workId时flowRecord为空
    private FlowRecord flowRecord;
    // 当前的流程设计器
    private Workflow workflow;
    // 流程节点记录
    private final List<ProcessNode> nodeList;
    // 流程审批记录列表
    private final Map<Long, IFlowOperator> recordOperatorMap;

    private final List<FlowRecord> recordList;


    public FlowProcessNodeService(FlowProcessNodeRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
        this.nodeList = new ArrayList<>();
        this.recordOperatorMap = new HashMap<>();
        this.recordList = new ArrayList<>();
        this.initData();
    }


    private void initData() {
        String id = this.request.getId();
        if (this.isCreateWorkflow()) {
            this.workflow = workflowService.getWorkflowByCode(id);
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

    private IFlowOperator loadRecordOperator(long operatorId) {
        IFlowOperator flowOperator = this.recordOperatorMap.get(operatorId);
        if (flowOperator != null) {
            return flowOperator;
        }

        flowOperator = this.repositoryHolder.getOperatorById(operatorId);
        if (flowOperator != null) {
            this.recordOperatorMap.put(flowOperator.getUserId(), flowOperator);
        }
        return flowOperator;
    }

    private void fetchFlowRecordOperatorList() {
        List<Long> operatorIds = new ArrayList<>();

        for (FlowRecord flowRecord : this.recordList) {
            if (!operatorIds.contains(flowRecord.getCreateOperatorId())) {
                operatorIds.add(flowRecord.getCreateOperatorId());
            }
            if (!operatorIds.contains(flowRecord.getCurrentOperatorId())) {
                operatorIds.add(flowRecord.getCurrentOperatorId());
            }
            if (!operatorIds.contains(flowRecord.getSubmitOperatorId())) {
                operatorIds.add(flowRecord.getSubmitOperatorId());
            }
        }

        List<IFlowOperator> operatorList = this.repositoryHolder.findOperatorByIds(operatorIds);
        if (operatorList != null && !operatorList.isEmpty()) {
            for (IFlowOperator operator : operatorList) {
                this.recordOperatorMap.put(operator.getUserId(), operator);
            }
        }
    }

    public List<ProcessNode> processNodes() {
        // load history data
        if (this.flowRecord != null) {
            this.loadHistoryData();
            if (this.flowRecord.isFinish()) {
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
        this.recordList.addAll(allRecords);

        this.fetchFlowRecordOperatorList();

        FlowRecordOrderService orderService = new FlowRecordOrderService(allRecords, this::loadRecordOperator, flowRecords -> nodeList.add(ProcessNode.createByRecord(flowRecords, workflow)));
        orderService.fetch(0);
    }

    private void loadEndNode(boolean finish) {
        IFlowNode endNode = this.workflow.getEndNode();
        this.nodeList.add(ProcessNode.createByEndNode(endNode, finish));
    }


    private List<FlowRecord> loadLatestRecords() {
        List<FlowRecord> flowRecords = new ArrayList<>();
        for (FlowRecord flowRecord : this.recordList) {
            if (flowRecord.isTodo() && !flowRecord.isHidden()) {
                flowRecords.add(flowRecord);
            }
        }
        return flowRecords;
    }


    private void loadNextData() {
        if (this.flowRecord == null) {
            IFlowNode currentNode = this.workflow.getStartNode();
            IFlowOperator currentOperator = this.loadRecordOperator(this.request.getOperatorId());
            FlowSession flowSession = this.buildFlowSession(null, currentNode, currentOperator, currentOperator, currentOperator, 0);
            List<FlowRecord> flowRecords = currentNode.generateCurrentRecords(flowSession);
            FlowRecord startRecord = flowRecords.get(0);
            flowSession.setCurrentRecord(startRecord);

            this.addFlowNode(currentNode, flowSession);
            this.fetchFlowNode(flowSession);
        } else {
            List<FlowRecord> todoLatestRecords = this.loadLatestRecords();
            IFlowOperator currentOperator = this.loadRecordOperator(this.request.getOperatorId());
            if (!todoLatestRecords.isEmpty()) {
                for (FlowRecord todoRecord : todoLatestRecords) {
                    IFlowNode currentNode = this.workflow.getFlowNode(todoRecord.getNodeId());
                    IFlowOperator createOperator = this.loadRecordOperator(todoRecord.getCreateOperatorId());
                    IFlowOperator submitOperator = this.loadRecordOperator(todoRecord.getSubmitOperatorId());

                    FlowSession flowSession = this.buildFlowSession(todoRecord, currentNode, currentOperator, createOperator, submitOperator, todoRecord.getWorkRuntimeId());
                    this.fetchFlowNode(flowSession);
                }
            }
        }
    }


    private FlowSession buildFlowSession(
            FlowRecord flowRecord,
            IFlowNode currentNode,
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
                flowRecord,
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
                    IFlowOperator currentOperator = this.loadRecordOperator(this.request.getOperatorId());
                    operators.add(currentOperator);
                    this.nodeList.add(ProcessNode.createByNode(flowNode, OperatorSelectType.SCRIPT, operators));
                } else {
                    OperatorManager operatorManager = flowNode.strategyManager().loadOperators(flowSession);
                    List<IFlowOperator> operators = operatorManager.getOperators();

                    OperatorSelectType operatorSelectType = null;
                    // 针对延迟节点、触发节点、子流程节点、路由节点、人工节点都没有设置流程审批人
                    OperatorLoadStrategy operatorLoadStrategy = flowNode.strategyManager().getStrategy(OperatorLoadStrategy.class);
                    if (operatorLoadStrategy != null) {
                        operatorSelectType = operatorLoadStrategy.getSelectType();
                    }

                    this.nodeList.add(ProcessNode.createByNode(flowNode, operatorSelectType, operators));
                }
            }
        }
    }


    private interface IFlowOperatorGateway {

        IFlowOperator getFlowOperator(long operatorId);
    }

    private static class FlowRecordOrderService {

        private final List<FlowRecord> flowRecords;

        private final Consumer<List<ProcessNode.FlowRecordOperator>> consumer;

        private final IFlowOperatorGateway flowOperatorGateway;


        public FlowRecordOrderService(List<FlowRecord> flowRecords, IFlowOperatorGateway flowOperatorGateway, Consumer<List<ProcessNode.FlowRecordOperator>> consumer) {
            this.consumer = consumer;
            this.flowOperatorGateway = flowOperatorGateway;
            this.flowRecords = flowRecords.stream().sorted(Comparator.comparing(FlowRecord::getId)).toList();
        }


        private List<FlowRecord> getNextRecords(long formId) {
            List<FlowRecord> recordList = new ArrayList<>();
            for (FlowRecord record : this.flowRecords) {
                if (record.getFromId() == formId) {
                    recordList.add(record);
                }
            }
            return recordList;
        }


        public void fetch(long formId) {
            List<FlowRecord> batchList = this.getNextRecords(formId);
            if (!batchList.isEmpty()) {
                // 根据nodeId 进行分组，不同的分组的要分开执行

                Map<String,List<FlowRecord>> groupList = this.loadGroupList(batchList);
                for(List<FlowRecord> group:groupList.values()) {

                    this.consumer.accept(group.stream().map(record -> new ProcessNode.FlowRecordOperator(record, flowOperatorGateway.getFlowOperator(record.getCurrentOperatorId()))).toList());

                    for (FlowRecord item : group) {
                        this.fetch(item.getId());
                    }
                }
            }
        }


        private Map<String, List<FlowRecord>> loadGroupList(List<FlowRecord> recordList) {
            Map<String, List<FlowRecord>> groupList = new HashMap<>();

            for (FlowRecord flowRecord : recordList) {
                String nodeId = flowRecord.getNodeId();

                List<FlowRecord> list = groupList.get(nodeId);
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(flowRecord);

                groupList.put(nodeId, list);
            }

            return groupList;
        }


    }

}
