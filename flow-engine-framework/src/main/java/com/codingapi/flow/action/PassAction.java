package com.codingapi.flow.action;

import com.codingapi.flow.node.IAuditNode;
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
    public List<FlowRecord> trigger(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        List<FlowRecord> records = new ArrayList<>();
        if (currentRecord.isReturnRecord()) {
            // 退回后的流程重新提交
            IAuditNode currentNode = flowSession.getWorkflow().getAuditNode(currentRecord.getReturnNodeId());
            StrategyManager strategyManager = currentNode.strategies();
            // 是否退回到退回节点
            if (strategyManager.isResume()) {
                FlowSession triggerSession = flowSession.updateSession(currentNode);
                List<FlowRecord> nextRecords = this.generateNextRecords(currentNode, triggerSession.updateSession(currentNode), currentRecord);
                records.addAll(nextRecords);
            }
        } else {
            List<IAuditNode> nextNodes = flowSession.nextNodes();
            for (IAuditNode node : nextNodes) {
                //TODO 如果是条件节点，则自动完成当前记录，并构建下一个记录
                List<FlowRecord> nextRecords = this.generateNextRecords(node, flowSession.updateSession(node), currentRecord);
                records.addAll(nextRecords);
            }
        }
        return records;
    }


}
