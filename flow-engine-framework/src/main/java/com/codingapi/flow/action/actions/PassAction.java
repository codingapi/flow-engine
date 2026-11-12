package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.actions.service.InitiatorSelectNodeService;
import com.codingapi.flow.context.ActionResponseContext;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.manager.FlowNodeState;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ConditionNode;
import com.codingapi.flow.node.nodes.ManualNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.NodeOption;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 通过
 */
public class PassAction extends BaseAction {

    public static final String DEFAULT_TITLE = "通过";


    public static PassAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, PassAction.class);
    }


    public static  PassAction defaultAction(){
        PassAction action = new PassAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.PASS.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        return action;
    }


    @Override
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        List<FlowRecord> records = new ArrayList<>();
        if (currentRecord.isReturnRecord()) {
            // 退回后的流程重新提交
            BaseAuditNode currentNode = (BaseAuditNode) flowSession.getWorkflow().getFlowNode(currentRecord.getReturnNodeId());
            NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
            // 是否退回到退回节点
            if (nodeStrategyManager.isResume()) {
                FlowSession triggerSession = flowSession.updateSession(currentNode);
                List<FlowRecord> nextRecords = currentNode.generateCurrentRecords(triggerSession.updateSession(currentNode));
                records.addAll(nextRecords);
            }
        } else {
            IFlowNode currentNode = flowSession.getCurrentNode();
            List<FlowRecord> nextRecords = currentNode.generateCurrentRecords(flowSession);
            if (!nextRecords.isEmpty()) {
                records.addAll(nextRecords);
            }
        }
        return records;
    }


    private List<NodeOption> loadInitiatorSelectNodes(FlowSession flowSession){
        InitiatorSelectNodeService initiatorSelectNodeService = new InitiatorSelectNodeService(flowSession);
        initiatorSelectNodeService.fetchNodes();
        return initiatorSelectNodeService.getOperatorSelectNodes();
    }


    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        IFlowNode currentNode = flowSession.getCurrentNode();

        // 如果下节点为手动节点时，提交流程时需要先选择节点
        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        if (nextNodes != null && nextNodes.size() == 1) {
            IFlowNode nextNode = nextNodes.get(0);
            if (nextNode.getType().equalsIgnoreCase(ManualNode.NODE_TYPE)) {
                IFlowNode manulNode = flowSession.getAdvice().getManualNode();
                if (manulNode == null && nextNode.blocks() != null) {
                    List<NodeOption> options = nextNode.blocks().stream().map(NodeOption::new).toList();
                    ActionResponseContext.getInstance().set(options);
                    return;
                }
            }
        }

        // 检查操作人选择：若存在需要手动设定操作人的节点且尚未设定，则提示用户
        List<NodeOption> operatorSelectNodes = new ArrayList<>();
        Map<String, List<Long>> operatorSelectMap = flowSession.getAdvice().getOperatorSelectMap();
        if (currentNode.getType().equalsIgnoreCase(StartNode.NODE_TYPE)) {
            // 发起人设定模式：开始节点提交时，扫描整个工作流中所有 INITIATOR_SELECT 节点
            operatorSelectNodes.addAll(this.loadInitiatorSelectNodes(flowSession));
            // 开始节点下游若为审批人设定（APPROVER_SELECT）节点，由于没有前序审批人可为其指定操作人，
            // 需由发起人在提交开始节点时指定，否则节点取不到审批人，流程会停滞在开始节点。
            // 必须与审批节点走同一套递归收集：人工节点选中分支后，下游实际是分支节点（MANUAL_BRANCH），
            // 只判断直接下游节点会漏掉分支内的 APPROVER_SELECT 节点（issue #259）
            if (nextNodes != null) {
                for (IFlowNode nextNode : nextNodes) {
                    collectApproverSelectNodes(flowSession, nextNode, operatorSelectMap, operatorSelectNodes);
                }
            }
        } else {
            // 审批人设定模式：审批节点提交时，检查下游节点是否有 APPROVER_SELECT
            if (nextNodes != null) {
                for (IFlowNode nextNode : nextNodes) {
                    collectApproverSelectNodes(flowSession, nextNode, operatorSelectMap, operatorSelectNodes);
                }
            }
        }

        if (!operatorSelectNodes.isEmpty()) {
            ActionResponseContext.getInstance().set(ActionResponse.ResponseType.OPERATOR_SELECT, operatorSelectNodes);
            return;
        }

        boolean isFinish = currentNode.isFinish(flowSession);
        currentRecord.update(flowSession, true);
        // 添加流程结束事件
        flowEvents.add(new FlowRecordDoneEvent(currentRecord, flowSession.isMock()));
        recordList.add(currentRecord);

        // 激活下一个按顺序审批的记录数据。
        // 委派记录只是临时办理人，原审批人尚未确认，不能提前激活后续顺序的审批人（issue #239）；
        // 只激活尚未展示过的记录，避免委派返回后重复激活已显示/已办理的人员。
        // 按「最近一条更大 nodeOrder」激活而不是固定的 nodeOrder + 1：减签撤销中间的隐藏审批人后
        // 序号会出现空洞，按 +1 查找会永远找不到下一位，导致后续审批人永不激活（issue #266）
        NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
        if (!currentRecord.isDelegate() && nodeStrategyManager.isSequenceMultiOperatorType()) {
            List<FlowRecord> currentRecords = flowSession.getCurrentNodeRecords();
            int nextNodeOrder = currentRecords.stream()
                    .filter(FlowRecord::isHidden)
                    .filter(record -> record.getNodeOrder() > currentRecord.getNodeOrder())
                    .mapToInt(FlowRecord::getNodeOrder)
                    .min()
                    .orElse(-1);
            if (nextNodeOrder >= 0) {
                for (FlowRecord record : currentRecords) {
                    if (record.getNodeOrder() == nextNodeOrder && record.isHidden()) {
                        record.show();
                        recordList.add(record);
                        flowEvents.add(new FlowRecordTodoEvent(record, flowSession.isMock()));
                    }
                }
            }
        }

        // 或签/并签：节点完成后，将同节点其他未办理的待办记录自动置为已办（已审核），
        // 避免其他审批人的遗留待办再次触发后续节点（issue #188 / #239）。
        // 委派记录通过后节点尚未真正完成（还需原审批人确认），此时不能办结其他待办。
        // 顺序审批（SEQUENCE）与随机一人（RANDOM_ONE）在节点完成时不会遗留同节点待办，
        // 且仓储按 (fromId, nodeId) 查询可能命中其他流程实例的开始节点记录（fromId=0），
        // 因此仅对或签/并签执行自动办结，并限定在同一流程实例内。
        MultiOperatorAuditStrategy.Type multiOperatorAuditType = nodeStrategyManager.getMultiOperatorAuditStrategyType();
        if (isFinish
                && !currentRecord.isDelegate()
                && (multiOperatorAuditType == MultiOperatorAuditStrategy.Type.ANY
                || multiOperatorAuditType == MultiOperatorAuditStrategy.Type.MERGE)) {
            for (FlowRecord record : flowSession.getCurrentNodeRecords()) {
                if (!currentRecord.getProcessId().equals(record.getProcessId())) {
                    continue;
                }
                if (record.getId() != currentRecord.getId() && record.isTodo()) {
                    record.autoDone();
                    recordList.add(record);
                    flowEvents.add(new FlowRecordDoneEvent(record, flowSession.isMock()));
                }
            }
        }

        // 先保存当前记录及其状态变更（含顺序激活、或签并签自动已办），
        // 再触发后续节点。子流程等嵌套执行在完成时会把当前记录标记为流程结束，
        // 若在触发完成后再统一保存，会以本会话中陈旧的"运行中"状态覆盖已更新的流程状态，
        // 导致最终流程记录停留在运行中（issue #184）。
        if (!recordList.isEmpty()) {
            repositoryHolder.saveRecords(recordList);
            recordList.clear();
        }

        // 是否转交审批人的流程
        if (isFinish && currentRecord.isForward()) {
            IFlowOperator forwardOperator = repositoryHolder.getOperatorById(currentRecord.getForwardOperatorId());
            FlowRecord notifyRecord = currentRecord.create(flowSession.updateSession(forwardOperator));
            notifyRecord.notifyRecord(flowSession.updateSession(forwardOperator));
            // 如果不存储这个记录，若下一流程是结束流程时，无法更新流程状态为结束状态。
            repositoryHolder.saveRecord(notifyRecord);
            flowEvents.add(new FlowRecordDoneEvent(notifyRecord, flowSession.isMock()));
        }

        // 委派记录办理完成后必须交还原审批人确认，与当前节点是否达到完成条件无关：
        // 多人审批时其他待办可能尚未完成，若只在 isFinish 时返回，委派记录会永久滞留（issue #239）
        if (currentRecord.isDelegate()) {
            FlowRecord delegateRecord = repositoryHolder.getRecordById(currentRecord.getDelegateId());
            IFlowOperator delegateOperator = repositoryHolder.getOperatorById(delegateRecord.getCurrentOperatorId());
            FlowRecord rebackRecord = delegateRecord.create(flowSession.updateSession(delegateOperator));
            // 返回记录同样归属原委托记录的节点实例与审批槽位（issue #239）
            rebackRecord.inheritNodeInstance(delegateRecord);
            rebackRecord.clearDelegate();

            recordList.add(rebackRecord);
            flowEvents.add(new FlowRecordTodoEvent(rebackRecord, flowSession.isMock()));
        } else if (isFinish) {
            this.triggerNode(flowSession, (triggerSession) -> {
                List<FlowRecord> records = this.generateRecords(triggerSession);
                if (!records.isEmpty()) {
                    for (FlowRecord record : records) {
                        if (record.isShow()) {
                            if (record.isNotify()) {
                                flowEvents.add(new FlowRecordDoneEvent(record, flowSession.isMock()));
                            } else {
                                flowEvents.add(new FlowRecordTodoEvent(record, flowSession.isMock()));
                            }
                        }
                    }
                    recordList.addAll(records);
                }
            });
        }
        repositoryHolder.saveRecords(recordList);

        // 结束节点的虚拟记录不持久化、状态为已完成（operatorId=-1），并非真实业务待办，
        // 其 Todo 事件在引擎内虽会生成，但推送业务订阅方前需拦截，避免下游收到"已完成流程的待办"。
        flowEvents.stream()
                .filter(event -> !isEndNodeTodoEvent(event))
                .forEach(EventPusher::push);
    }

    /**
     * 收集需要审批人设定的下游节点。
     *
     * <p>仅沿本次提交后实际会执行到的节点递归，避免把分支内更靠后的节点提前纳入提示（issue #231）：
     * <ul>
     *     <li>控制节点（条件、并行、包容、人工）：进入其下各分支，条件节点仅沿命中的分支（issue #200）</li>
     *     <li>分支节点：分支内是线性执行链，只检查排在最前的首个节点；
     *     分支内的后续节点由该分支上前一个节点提交时再提示</li>
     * </ul>
     */
    private void collectApproverSelectNodes(FlowSession flowSession, IFlowNode node, Map<String, List<Long>> operatorSelectMap, List<NodeOption> result) {
        OperatorSelectType selectType = node.strategyManager().getOperatorSelectType();
        if (selectType == OperatorSelectType.APPROVER_SELECT) {
            if (operatorSelectMap == null || !operatorSelectMap.containsKey(node.getId())
                    || operatorSelectMap.get(node.getId()).isEmpty()) {
                List<IFlowOperator> range = node.strategyManager()
                        .loadOperatorRange(flowSession.updateSession(node));
                result.add(new NodeOption(node, range));
            }
        }
        List<IFlowNode> blocks = node.blocks();
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        FlowNodeState nodeState = new FlowNodeState(node);
        if (nodeState.isBlockNode()) {
            // 控制节点：递归进入各分支；条件节点仅沿实际命中的分支递归，
            // 避免把不会执行的分支中的审批人设定节点误加入提示（issue #200）
            if (node instanceof ConditionNode) {
                blocks = blocks.get(0).filterBranches(blocks, flowSession);
            }
            for (IFlowNode block : blocks) {
                collectApproverSelectNodes(flowSession, block, operatorSelectMap, result);
            }
        } else if (nodeState.isBranchNode()) {
            // 分支节点：仅检查分支的首个节点，分支内后续节点由前序节点提交时再提示（issue #231）
            for (IFlowNode firstNode : nodeState.getFirstBlocks()) {
                collectApproverSelectNodes(flowSession, firstNode, operatorSelectMap, result);
            }
        }
    }
}
