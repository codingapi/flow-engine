package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.MultiOperatorAuditStrategy;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *   通过
 */
public class PassAction extends BaseAction {

    public PassAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "通过";
        this.type = ActionType.PASS;
        this.display = new ActionDisplay(this.title);
    }

    public static PassAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, PassAction.class);
    }

    @Override
    public boolean isDone(FlowSession session, FlowRecord currentRecord, List<FlowRecord> currentRecords) {
        // 多人审批
        if (currentRecords.size() > 1) {
            StrategyManager strategyManager = session.getCurrentNode().strategies();
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = strategyManager.getMultiOperatorAuditStrategyType();
            // 顺序审批
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.SEQUENCE) {
                int currentOrder = currentRecord.getNodeOrder();
                int maxNodeOrder = currentRecords.size() - 1;
                return currentOrder >= maxNodeOrder;
            }
            // 或签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.ANY) {
                return true;
            }
            // 并签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.MERGE) {
                float percent = strategyManager.getMultiOperatorAuditMergePercent();
                long total = currentRecords.size();
                // 尚未办理的数量为所有待办数-1，1是当前办理的这条记录
                long todoCount = currentRecords.stream().filter(FlowRecord::isTodo).count() - 1;
                long doneCount = total - todoCount;
                return doneCount >= total * percent;
            }
        }
        return true;
    }

    @Override
    public List<FlowRecord> trigger(FlowSession flowSession, FlowRecord currentRecord) {
        List<FlowRecord> records = new ArrayList<>();
        if (currentRecord.isReturnRecord()) {
            // 退回后的流程重新提交
            IFlowNode currentNode = flowSession.getWorkflow().getNode(currentRecord.getReturnNodeId());
            StrategyManager strategyManager = currentNode.strategies();
            // 是否退回到退回节点
            if (strategyManager.isResume()) {
                FlowSession triggerSession = flowSession.updateSession(currentNode);
                List<FlowRecord> nextRecords = this.generateNextRecords(currentNode, triggerSession.updateSession(currentNode), currentRecord);
                records.addAll(nextRecords);
            }
        } else {
            List<IFlowNode> nextNodes = flowSession.nextNode();
            for (IFlowNode node : nextNodes) {
                List<FlowRecord> nextRecords = this.generateNextRecords(node, flowSession.updateSession(node), currentRecord);
                records.addAll(nextRecords);
            }
        }
        return records;
    }


}
