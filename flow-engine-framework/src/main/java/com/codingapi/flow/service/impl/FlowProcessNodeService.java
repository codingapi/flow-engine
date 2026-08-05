package com.codingapi.flow.service.impl;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.domain.SubProcessContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.node.IBlockNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.node.nodes.RouterNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.node.nodes.SubProcessNode;
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
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

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
    // 已产生流程记录对应的节点列表
    private final List<ProcessNode> historyNodeList;
    // 尚未产生记录、根据当前流程状态预览的节点列表
    private final List<ProcessNode> previewNodeList;
    // 工作流树按块深度优先展开后的展示顺序
    private final Map<String, Integer> displayNodeOrderMap;

    // 流程审批记录列表
    private final Map<Long, IFlowOperator> recordOperatorMap;

    private final List<FlowRecord> recordList;
    private final List<SubProcessRecord> subProcessRecordList;
    private final Map<Long, String> subProcessWorkTitleMap;


    public FlowProcessNodeService(FlowProcessNodeRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
        this.historyNodeList = new ArrayList<>();
        this.previewNodeList = new ArrayList<>();
        this.displayNodeOrderMap = new HashMap<>();
        this.recordOperatorMap = new HashMap<>();
        this.recordList = new ArrayList<>();
        this.subProcessRecordList = new ArrayList<>();
        this.subProcessWorkTitleMap = new HashMap<>();
        this.initData();
        this.initDisplayNodeOrder();
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
            FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
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
        ParentSubProcessContext parentContext = this.loadParentSubProcessContext(this.flowRecord);
        if (parentContext != null && parentContext.showParentProcessRecords()) {
            this.loadHistoryData();
            this.loadEndNode(this.flowRecord.isFinish());
            List<ProcessNode> processNodes = new ArrayList<>(this.loadParentHistory(parentContext));
            processNodes.addAll(this.buildProcessNodeList());
            return processNodes;
        }

        // load history data
        if (this.flowRecord != null) {
            this.loadHistoryData();
            if (this.flowRecord.isFinish()) {
                // load end node
                this.loadEndNode(this.flowRecord.isFinish());
                return this.buildProcessNodeList();
            }
        }
        // load next node data
        this.loadNextData();
        // load end node
        this.loadEndNode(false);
        return this.buildProcessNodeList();
    }

    /**
     * 将树形流程定义展开为稳定的列表顺序。
     * 同一层的块按 order 排序，每个块内部采用深度优先顺序。
     */
    private void initDisplayNodeOrder() {
        int[] order = {0};
        this.indexDisplayNodes(this.workflow.getNodes(), order, false);
    }

    private void indexDisplayNodes(List<IFlowNode> nodes, int[] order, boolean sortByOrder) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        List<IFlowNode> sortedNodes = sortByOrder
                ? nodes.stream().sorted(Comparator.comparingInt(IFlowNode::getOrder)).toList()
                : nodes;
        for (IFlowNode node : sortedNodes) {
            if (node instanceof IDisplayNode) {
                this.displayNodeOrderMap.putIfAbsent(node.getId(), order[0]++);
            }
            // 只有控制节点的直属 blocks 是互斥/并行分支，需要按 order 排列；
            // 顶层节点和分支内部节点是线性拓扑，必须保持定义列表顺序。
            this.indexDisplayNodes(node.blocks(), order, node instanceof IBlockNode);
        }
    }

    private int displayNodeOrder(String nodeId) {
        return this.displayNodeOrderMap.getOrDefault(nodeId, Integer.MAX_VALUE);
    }

    /**
     * 合并历史节点与未来预览节点。
     *
     * <p>普通树形执行中，按工作流定义顺序合并并按 nodeId 去重，保证并行分支完整展示后
     * 再展示下一分支，且共享的汇聚节点只出现一次。</p>
     *
     * <p>退回等场景会在历史中真实产生相同 nodeId 的多次执行记录，此时必须保留历史执行链，
     * 只对未来预览部分去重并排序，避免破坏 A -> B -> A -> B 这类历史语义。</p>
     */
    private List<ProcessNode> buildProcessNodeList() {
        if (this.hasRepeatedHistoryNode()) {
            return this.buildRepeatedHistoryProcessNodeList();
        }

        Map<String, ProcessNode> nodeMap = new LinkedHashMap<>();
        for (ProcessNode node : this.historyNodeList) {
            nodeMap.putIfAbsent(node.getNodeId(), node);
        }
        for (ProcessNode node : this.previewNodeList) {
            nodeMap.putIfAbsent(node.getNodeId(), node);
        }
        return this.sortProcessNodes(nodeMap.values());
    }

    private boolean hasRepeatedHistoryNode() {
        Set<String> nodeIds = new HashSet<>();
        for (ProcessNode node : this.historyNodeList) {
            if (!nodeIds.add(node.getNodeId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 退回会产生 A -> B -> A -> B 形式的重复历史。重复区间内部必须保持真实执行顺序，
     * 区间之外的历史和预览仍应一起按流程定义的树形顺序排列。
     *
     * <p>异常回退可能同时留下 A(已办)、A(待办) 和 D1(待办)。从待办 A 预览时还会再次
     * 遍历到 D1，因此需要移除与真实处理中节点重复的预览，但保留与已办历史重复的未来节点，
     * 例如 B1 -> B2 -> B1 后仍需展示下一轮 B2。</p>
     */
    private List<ProcessNode> buildRepeatedHistoryProcessNodeList() {
        List<DisplayOrderRange> repeatedRanges = this.loadRepeatedHistoryRanges();
        Set<String> processingHistoryNodeIds = this.historyNodeList.stream()
                .filter(node -> node.getApproveState() == ProcessNode.ApproveState.PROCESSING)
                .map(ProcessNode::getNodeId)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, ProcessNode> previewNodeMap = new LinkedHashMap<>();
        for (ProcessNode previewNode : this.previewNodeList) {
            if (!processingHistoryNodeIds.contains(previewNode.getNodeId())) {
                previewNodeMap.putIfAbsent(previewNode.getNodeId(), previewNode);
            }
        }

        List<ProcessNode> result = new ArrayList<>(this.historyNodeList);
        result.addAll(previewNodeMap.values());
        return result.stream()
                .sorted(Comparator.comparingInt(node -> this.historyDisplayOrder(node, repeatedRanges)))
                .toList();
    }

    private List<DisplayOrderRange> loadRepeatedHistoryRanges() {
        Map<String, List<Integer>> positions = new HashMap<>();
        for (int i = 0; i < this.historyNodeList.size(); i++) {
            positions.computeIfAbsent(this.historyNodeList.get(i).getNodeId(), key -> new ArrayList<>()).add(i);
        }

        List<DisplayOrderRange> ranges = new ArrayList<>();
        for (List<Integer> repeatedPositions : positions.values()) {
            if (repeatedPositions.size() < 2) {
                continue;
            }
            int first = repeatedPositions.get(0);
            int last = repeatedPositions.get(repeatedPositions.size() - 1);
            int minOrder = Integer.MAX_VALUE;
            int maxOrder = Integer.MIN_VALUE;
            for (int i = first; i <= last; i++) {
                int displayOrder = this.displayNodeOrder(this.historyNodeList.get(i).getNodeId());
                minOrder = Math.min(minOrder, displayOrder);
                maxOrder = Math.max(maxOrder, displayOrder);
            }
            ranges.add(new DisplayOrderRange(minOrder, maxOrder));
        }

        return this.mergeRanges(ranges);
    }

    private int historyDisplayOrder(ProcessNode node, List<DisplayOrderRange> ranges) {
        int displayOrder = this.displayNodeOrder(node.getNodeId());
        for (DisplayOrderRange range : ranges) {
            if (range.contains(displayOrder)) {
                return range.start();
            }
        }
        return displayOrder;
    }

    private List<DisplayOrderRange> mergeRanges(List<DisplayOrderRange> ranges) {
        List<DisplayOrderRange> sortedRanges = ranges.stream()
                .sorted(Comparator.comparingInt(DisplayOrderRange::start))
                .toList();
        List<DisplayOrderRange> mergedRanges = new ArrayList<>();
        for (DisplayOrderRange range : sortedRanges) {
            if (mergedRanges.isEmpty()) {
                mergedRanges.add(range);
                continue;
            }
            DisplayOrderRange previous = mergedRanges.get(mergedRanges.size() - 1);
            if (range.start() <= previous.end()) {
                mergedRanges.set(mergedRanges.size() - 1,
                        new DisplayOrderRange(previous.start(), Math.max(previous.end(), range.end())));
            } else {
                mergedRanges.add(range);
            }
        }
        return mergedRanges;
    }

    private record DisplayOrderRange(int start, int end) {
        private boolean contains(int order) {
            return order >= start && order <= end;
        }
    }

    private List<ProcessNode> sortProcessNodes(Collection<ProcessNode> nodes) {
        return nodes.stream()
                .sorted(Comparator.comparingInt(node -> this.displayNodeOrder(node.getNodeId())))
                .toList();
    }


    private void loadHistoryData() {
        List<FlowRecord> allRecords = flowRecordService.findFlowRecordByProcessId(this.flowRecord.getProcessId());
        this.recordList.addAll(allRecords);
        List<SubProcessRecord> subProcessRecords = repositoryHolder.getSubProcessRepository()
                .findByParentProcessId(this.flowRecord.getProcessId());
        this.subProcessRecordList.addAll(subProcessRecords);
        this.subProcessWorkTitleMap.putAll(this.loadSubProcessWorkTitles(subProcessRecords));

        this.fetchFlowRecordOperatorList();

        FlowRecordOrderService orderService = new FlowRecordOrderService(
                allRecords,
                subProcessRecords,
                this::loadRecordOperator,
                flowRecords -> historyNodeList.add(ProcessNode.createByRecord(flowRecords, workflow)),
                subProcessRecord -> historyNodeList.add(
                        ProcessNode.createBySubProcessRecord(
                                subProcessRecord, workflow, subProcessWorkTitleMap::get)),
                this::displayNodeOrder);
        orderService.fetch(0);
    }

    private ParentSubProcessContext loadParentSubProcessContext(FlowRecord childRecord) {
        if (childRecord == null || childRecord.getParentId() <= 0) {
            return null;
        }
        FlowRecord parentRecord = flowRecordService.getFlowRecord(childRecord.getParentId());
        if (parentRecord == null) {
            return null;
        }
        SubProcessRecord subProcessRecord = repositoryHolder.getSubProcessRepository()
                .findByParentRecordId(parentRecord.getId()).stream()
                .filter(record -> record.containsChildProcess(childRecord.getProcessId()))
                .findFirst()
                .orElse(null);
        if (subProcessRecord == null) {
            return null;
        }
        WorkflowRuntime parentRuntime = workflowService.getWorkflowRuntime(
                subProcessRecord.getParentWorkRuntimeId());
        if (parentRuntime == null) {
            return null;
        }
        Workflow parentWorkflow = parentRuntime.toWorkflow();
        IFlowNode parentNode = parentWorkflow.getFlowNode(subProcessRecord.getNodeId());
        if (!(parentNode instanceof SubProcessNode)) {
            return null;
        }
        SubProcessStrategy strategy = parentNode.strategyManager().getStrategy(SubProcessStrategy.class);
        boolean showParentProcessRecords = strategy != null && strategy.isShowParentProcessRecords();
        return new ParentSubProcessContext(
                parentRecord, subProcessRecord, parentWorkflow, showParentProcessRecords);
    }

    /**
     * 只根据持久化记录的 fromId 链构造主流程历史，不执行主流程后续节点推演。
     */
    private List<ProcessNode> loadParentHistory(ParentSubProcessContext context) {
        List<ProcessNode> processNodes = new ArrayList<>();
        ParentSubProcessContext ancestorContext = this.loadParentSubProcessContext(context.parentRecord());
        if (ancestorContext != null && ancestorContext.showParentProcessRecords()) {
            processNodes.addAll(this.loadParentHistory(ancestorContext));
        }

        List<FlowRecord> parentRecords = flowRecordService.findFlowRecordByProcessId(
                context.parentRecord().getProcessId());
        this.fetchFlowRecordOperatorList(parentRecords);
        for (List<FlowRecord> recordGroup : this.loadRecordPath(context.parentRecord(), parentRecords)) {
            List<ProcessNode.FlowRecordOperator> operators = recordGroup.stream()
                    .map(record -> new ProcessNode.FlowRecordOperator(
                            record, this.loadRecordOperator(record.getCurrentOperatorId())))
                    .toList();
            ProcessNode parentProcessNode = ProcessNode.createByRecord(operators, context.parentWorkflow());
            parentProcessNode.setParentProcessRecord(true);
            processNodes.add(parentProcessNode);
        }

        Map<Long, String> workTitles = this.loadSubProcessWorkTitles(List.of(context.subProcessRecord()));
        ProcessNode parentSubProcessNode = ProcessNode.createBySubProcessRecord(
                context.subProcessRecord(), context.parentWorkflow(), workTitles::get);
        parentSubProcessNode.setParentProcessRecord(true);
        processNodes.add(parentSubProcessNode);
        return processNodes;
    }

    private List<List<FlowRecord>> loadRecordPath(FlowRecord targetRecord, List<FlowRecord> allRecords) {
        Map<Long, FlowRecord> recordMap = allRecords.stream()
                .collect(java.util.stream.Collectors.toMap(FlowRecord::getId, record -> record));
        List<List<FlowRecord>> reversePath = new ArrayList<>();
        FlowRecord currentRecord = targetRecord;
        while (currentRecord != null) {
            FlowRecord pathRecord = currentRecord;
            List<FlowRecord> recordGroup = allRecords.stream()
                    .filter(record -> record.getFromId() == pathRecord.getFromId())
                    .filter(record -> record.getNodeId().equals(pathRecord.getNodeId()))
                    .sorted(Comparator.comparingLong(FlowRecord::getId))
                    .toList();
            reversePath.add(recordGroup.isEmpty() ? List.of(currentRecord) : recordGroup);
            if (currentRecord.getFromId() == 0) {
                break;
            }
            currentRecord = recordMap.get(currentRecord.getFromId());
        }
        Collections.reverse(reversePath);
        return reversePath;
    }

    private Map<Long, String> loadSubProcessWorkTitles(List<SubProcessRecord> records) {
        List<Long> startRecordIds = records.stream()
                .flatMap(record -> record.getInstances().stream())
                .filter(instance -> instance.getWorkTitle() == null || instance.getWorkTitle().isBlank())
                .map(SubProcessRecord.Instance::getStartRecordId)
                .distinct()
                .toList();
        if (startRecordIds.isEmpty()) {
            return Map.of();
        }
        return flowRecordService.findFlowRecordByIds(startRecordIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        FlowRecord::getId, FlowRecord::getWorkTitle, (left, right) -> left));
    }

    private void fetchFlowRecordOperatorList(List<FlowRecord> records) {
        List<Long> operatorIds = records.stream()
                .flatMap(record -> java.util.stream.Stream.of(
                        record.getCreateOperatorId(),
                        record.getCurrentOperatorId(),
                        record.getSubmitOperatorId()))
                .filter(operatorId -> operatorId > 0 && !this.recordOperatorMap.containsKey(operatorId))
                .distinct()
                .toList();
        if (operatorIds.isEmpty()) {
            return;
        }
        List<IFlowOperator> operatorList = this.repositoryHolder.findOperatorByIds(operatorIds);
        if (operatorList != null) {
            operatorList.forEach(operator -> this.recordOperatorMap.put(operator.getUserId(), operator));
        }
    }

    private record ParentSubProcessContext(FlowRecord parentRecord,
                                           SubProcessRecord subProcessRecord,
                                           Workflow parentWorkflow,
                                           boolean showParentProcessRecords) {
    }

    private void loadEndNode(boolean finish) {
        IFlowNode endNode = this.workflow.getEndNode();
        this.previewNodeList.add(ProcessNode.createByEndNode(endNode, finish));
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
                // 同一节点存在多条待办（如会签/或签）时，向后预览只需按节点遍历一次，
                // 否则下游节点会随待办条数被重复展示
                Map<String, FlowRecord> nodeTodoMap = new LinkedHashMap<>();
                List<FlowRecord> sortedTodoRecords = todoLatestRecords.stream()
                        .sorted(Comparator.comparingInt(record -> this.displayNodeOrder(record.getNodeId())))
                        .toList();
                for (FlowRecord todoRecord : sortedTodoRecords) {
                    nodeTodoMap.putIfAbsent(todoRecord.getNodeId(), todoRecord);
                }
                for (FlowRecord todoRecord : nodeTodoMap.values()) {
                    IFlowNode currentNode = this.workflow.getFlowNode(todoRecord.getNodeId());
                    IFlowOperator createOperator = this.loadRecordOperator(todoRecord.getCreateOperatorId());
                    IFlowOperator submitOperator = this.loadRecordOperator(todoRecord.getSubmitOperatorId());

                    FlowSession flowSession = this.buildFlowSession(todoRecord, currentNode, currentOperator, createOperator, submitOperator, todoRecord.getWorkRuntimeId());
                    this.fetchFlowNodeReadOnly(flowSession);
                }
            }
            this.loadWaitingSubProcessNextData(currentOperator);
        }
    }

    private void loadWaitingSubProcessNextData(IFlowOperator currentOperator) {
        List<SubProcessRecord> waitingRecords = this.subProcessRecordList.stream()
                .filter(SubProcessRecord::isWaiting)
                .sorted(Comparator
                        .comparingInt((SubProcessRecord record) -> this.displayNodeOrder(record.getNodeId()))
                        .thenComparingLong(SubProcessRecord::getId))
                .toList();
        if (waitingRecords.isEmpty()) {
            return;
        }

        Map<Long, FlowRecord> parentRecordMap = this.recordList.stream()
                .collect(java.util.stream.Collectors.toMap(FlowRecord::getId, record -> record));
        for (SubProcessRecord subProcessRecord : waitingRecords) {
            FlowRecord parentRecord = parentRecordMap.get(subProcessRecord.getParentRecordId());
            if (parentRecord == null) {
                continue;
            }
            IFlowNode sourceNode = this.workflow.getFlowNode(parentRecord.getNodeId());
            IFlowAction sourceAction = sourceNode.actionManager().getActionById(parentRecord.getActionId());
            IFlowNode subProcessNode = this.workflow.getFlowNode(subProcessRecord.getNodeId());
            IFlowOperator createOperator = this.loadRecordOperator(parentRecord.getCreateOperatorId());
            IFlowOperator submitOperator = this.loadRecordOperator(parentRecord.getSubmitOperatorId());

            FormData formData = new FormData(this.workflow.getForm());
            formData.reset(this.request.getFormData());
            FlowSession flowSession = new FlowSession(
                    this.repositoryHolder,
                    currentOperator,
                    createOperator,
                    submitOperator,
                    this.workflow,
                    subProcessNode,
                    sourceAction,
                    formData,
                    parentRecord,
                    new ArrayList<>(),
                    parentRecord.getWorkRuntimeId(),
                    FlowAdvice.nullFlowAdvice()
            );
            flowSession.setSubProcessContext(new SubProcessContext(subProcessRecord, null));
            this.fetchFlowNodeReadOnly(flowSession);
        }
    }

    /**
     * 节点预览会执行分支过滤逻辑。并行、包容分支在运行态会把汇聚信息写入当前记录，
     * 但 ProcessNodes 属于只读查询，不能因此改写持久化记录并影响后续实际汇聚。
     */
    private void fetchFlowNodeReadOnly(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        if (currentRecord == null) {
            this.fetchFlowNode(flowSession);
            return;
        }

        String parallelId = currentRecord.getParallelId();
        String parallelBranchNodeId = currentRecord.getParallelBranchNodeId();
        int parallelBranchTotal = currentRecord.getParallelBranchTotal();
        try {
            this.fetchFlowNode(flowSession);
        } finally {
            currentRecord.setParallelId(parallelId);
            currentRecord.setParallelBranchNodeId(parallelBranchNodeId);
            currentRecord.setParallelBranchTotal(parallelBranchTotal);
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
        // Router 的过滤策略会直接返回目标节点，常规 matchNextNodes 会跳过 Router 本身。
        // Router 属于展示节点，预览时应先保留该配置节点，再继续解析其动态目标。
        List<IFlowNode> configuredNextNodes = this.workflow.nextNodes(flowSession.getCurrentNode());
        if (configuredNextNodes != null && configuredNextNodes.size() == 1
                && configuredNextNodes.get(0) instanceof RouterNode routerNode) {
            FlowSession routerSession = flowSession.updateSession(routerNode);
            this.addFlowNode(routerNode, routerSession);
            this.fetchFlowNode(routerSession);
            return;
        }

        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        if (nextNodes != null && !nextNodes.isEmpty()) {
            for (IFlowNode flowNode : nextNodes) {
                FlowSession nextSession = flowSession.updateSession(flowNode);
                this.addFlowNode(flowNode, nextSession);
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
                    this.previewNodeList.add(ProcessNode.createByNode(flowNode, OperatorSelectType.SCRIPT, operators));
                } else {
                    OperatorManager operatorManager = flowNode.strategyManager().loadOperators(flowSession);
                    List<IFlowOperator> operators = operatorManager.getOperators();

                    OperatorSelectType operatorSelectType = null;
                    // 针对延迟节点、触发节点、子流程节点、路由节点、人工节点都没有设置流程审批人
                    OperatorLoadStrategy operatorLoadStrategy = flowNode.strategyManager().getStrategy(OperatorLoadStrategy.class);
                    if (operatorLoadStrategy != null) {
                        operatorSelectType = operatorLoadStrategy.getSelectType();
                    }

                    this.previewNodeList.add(ProcessNode.createByNode(flowNode, operatorSelectType, operators));
                }
            }
        }
    }


    private interface IFlowOperatorGateway {

        IFlowOperator getFlowOperator(long operatorId);
    }

    private static class FlowRecordOrderService {

        private final List<FlowRecord> flowRecords;
        private final List<SubProcessRecord> subProcessRecords;

        private final Consumer<List<ProcessNode.FlowRecordOperator>> consumer;
        private final Consumer<SubProcessRecord> subProcessConsumer;

        private final IFlowOperatorGateway flowOperatorGateway;

        private final ToIntFunction<String> nodeOrder;


        public FlowRecordOrderService(List<FlowRecord> flowRecords,
                                      List<SubProcessRecord> subProcessRecords,
                                      IFlowOperatorGateway flowOperatorGateway,
                                      Consumer<List<ProcessNode.FlowRecordOperator>> consumer,
                                      Consumer<SubProcessRecord> subProcessConsumer,
                                      ToIntFunction<String> nodeOrder) {
            this.consumer = consumer;
            this.subProcessConsumer = subProcessConsumer;
            this.flowOperatorGateway = flowOperatorGateway;
            this.nodeOrder = nodeOrder;
            this.flowRecords = flowRecords.stream().sorted(Comparator.comparing(FlowRecord::getId)).toList();
            this.subProcessRecords = subProcessRecords.stream()
                    .sorted(Comparator.comparing(SubProcessRecord::getId))
                    .toList();
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

        private List<SubProcessRecord> getNextSubProcessRecords(long parentRecordId) {
            return this.subProcessRecords.stream()
                    .filter(record -> record.getParentRecordId() == parentRecordId)
                    .toList();
        }


        public void fetch(long formId) {
            List<FlowRecord> batchList = this.getNextRecords(formId);
            List<SubProcessRecord> subProcessList = this.getNextSubProcessRecords(formId);
            if (batchList.isEmpty() && subProcessList.isEmpty()) {
                return;
            }

            // 子流程恢复后的下游记录仍以触发记录作为 fromId，因此二者在记录树上是兄弟节点。
            // 实际语义上子流程执行先于恢复/异常跳转产生的记录，必须先插入子流程记录。
            subProcessList.stream()
                    .sorted(Comparator
                            .comparingInt((SubProcessRecord record) -> this.nodeOrder.applyAsInt(record.getNodeId()))
                            .thenComparingLong(SubProcessRecord::getId))
                    .forEach(this.subProcessConsumer);

            Map<String, List<FlowRecord>> groupList = this.loadGroupList(batchList);
            for (List<FlowRecord> group : groupList.values()) {
                this.consumer.accept(group.stream()
                        .map(record -> new ProcessNode.FlowRecordOperator(
                                record, flowOperatorGateway.getFlowOperator(record.getCurrentOperatorId())))
                        .toList());
                for (FlowRecord item : group) {
                    this.fetch(item.getId());
                }
            }
        }


        private Map<String, List<FlowRecord>> loadGroupList(List<FlowRecord> recordList) {
            Map<String, List<FlowRecord>> groupList = new LinkedHashMap<>();

            List<FlowRecord> sortedRecords = recordList.stream()
                    .sorted(Comparator
                            .comparingInt((FlowRecord record) -> this.nodeOrder.applyAsInt(record.getNodeId()))
                            .thenComparingLong(FlowRecord::getId))
                    .toList();
            for (FlowRecord flowRecord : sortedRecords) {
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
