package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<FlowRecord> trigger(FlowSession flowSession,FlowRecord currentRecord) {
        if(currentRecord.isReturnRecord()){
            // 退回后的流程重新提交
            IFlowNode currentNode = flowSession.getWorkflow().getNode(currentRecord.getReturnNodeId());
            System.out.println(currentNode.getType());
            //TODO 如果当前流程是退回的记录，则需要根据节点设置判断如何继续执行
        }

        List<IFlowNode> nextNodes = flowSession.nextNode();
        List<FlowRecord> records = new ArrayList<>();
        for (IFlowNode node : nextNodes) {
            FlowSession triggerSession = flowSession.updateSession(node);
            OperatorManager operatorManager = node.operators(triggerSession);
            for(IFlowOperator operator : operatorManager.getOperators()) {
                FlowRecord flowRecord = new FlowRecord(triggerSession.updateSession(operator), this.id, currentRecord.getProcessId(), currentRecord.getId());
                records.add(flowRecord);
            }
        }
        return records;
    }
}
