package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.NodeOperators;
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
        this.order = 1;
        this.display = new ActionDisplay(this.title);
    }

    public static PassAction formMap(Map<String, Object> data) {
        return BaseAction.formMap(data, PassAction.class);
    }

    @Override
    public List<FlowRecord> trigger(FlowSession flowSession,FlowRecord currentRecord) {
        List<IFlowNode> nextNodes = flowSession.nextNode();
        List<FlowRecord> records = new ArrayList<>();
        for (IFlowNode node : nextNodes) {
            FlowSession triggerSession = flowSession.updateSession(node);
            NodeOperators nodeOperators = node.operators(triggerSession);
            for(IFlowOperator operator : nodeOperators.getOperators()) {
                FlowRecord flowRecord = new FlowRecord(triggerSession.updateSession(operator), this.id, currentRecord.getProcessId(), currentRecord.getId());
                records.add(flowRecord);
            }
        }
        return records;
    }
}
