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
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.ManualNode;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.NodeOption;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
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
        if (currentNode.getType().equalsIgnoreCase(StartNode.NODE_TYPE)) {
            // 发起人设定模式：开始节点提交时，扫描整个工作流中所有 INITIATOR_SELECT 节点
            operatorSelectNodes.addAll(this.loadInitiatorSelectNodes(flowSession));
        } else {
            Map<String, List<Long>> operatorSelectMap = flowSession.getAdvice().getOperatorSelectMap();
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

        // 激活下一个按顺序审批的记录数据
        NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
        if (nodeStrategyManager.isSequenceMultiOperatorType()) {
            List<FlowRecord> currentRecords = flowSession.getCurrentNodeRecords();
            for (FlowRecord record : currentRecords) {
                if (record.getNodeOrder() == currentRecord.getNodeOrder() + 1) {
                    record.show();
                    recordList.add(record);
                    flowEvents.add(new FlowRecordTodoEvent(record, flowSession.isMock()));
                }
            }
        }

        if (isFinish) {
            // 是否转交审批人的流程
            if (currentRecord.isForward()) {
                IFlowOperator forwardOperator = repositoryHolder.getOperatorById(currentRecord.getForwardOperatorId());
                FlowRecord notifyRecord = currentRecord.create(flowSession.updateSession(forwardOperator));
                notifyRecord.notifyRecord(flowSession.updateSession(forwardOperator));
                // 如果不存储这个记录，若下一流程是结束流程时，无法更新流程状态为结束状态。
                repositoryHolder.saveRecord(notifyRecord);
                flowEvents.add(new FlowRecordDoneEvent(notifyRecord, flowSession.isMock()));
            }

            // 是否委托记录
            if (currentRecord.isDelegate()) {
                FlowRecord delegateRecord = repositoryHolder.getRecordById(currentRecord.getDelegateId());
                IFlowOperator delegateOperator = repositoryHolder.getOperatorById(delegateRecord.getCurrentOperatorId());
                FlowRecord rebackRecord = delegateRecord.create(flowSession.updateSession(delegateOperator));
                rebackRecord.clearDelegate();

                recordList.add(rebackRecord);
                flowEvents.add(new FlowRecordTodoEvent(rebackRecord, flowSession.isMock()));
            } else {
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
        }
        repositoryHolder.saveRecords(recordList);

        flowEvents.forEach(EventPusher::push);
    }

    /**
     * 收集需要审批人设定的下游节点
     * 若下游节点本身是控制节点（如条件节点），则递归检查其子节点
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
        // 若节点包含子块（如条件分支），递归检查其子节点
        if (node.blocks() != null) {
            for (IFlowNode block : node.blocks()) {
                collectApproverSelectNodes(flowSession, block, operatorSelectMap, result);
            }
        }
    }
}
