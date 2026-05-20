package com.codingapi.flow.action.actions.service;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.NodeOption;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitiatorSelectNodeService {

    @Getter
    private final List<NodeOption> operatorSelectNodes = new ArrayList<>();
    private final Map<String, List<Long>> operatorSelectMap;
    private final FlowSession flowSession;

    public InitiatorSelectNodeService(FlowSession flowSession) {
        this.flowSession = flowSession;
        this.operatorSelectMap = flowSession.getAdvice().getOperatorSelectMap();
    }


    public void fetchNodes() {
        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        this.appendNodes(nextNodes);
    }


    private void appendNodes(List<IFlowNode> currentNodes) {
        if(currentNodes!=null) {
            for (IFlowNode node : currentNodes) {
                OperatorSelectType selectType = node.strategyManager().getOperatorSelectType();
                if (selectType == OperatorSelectType.INITIATOR_SELECT) {
                    if (operatorSelectMap == null || !operatorSelectMap.containsKey(node.getId())
                            || operatorSelectMap.get(node.getId()).isEmpty()) {
                        List<IFlowOperator> range = node.strategyManager()
                                .loadOperatorRange(this.flowSession.updateSession(node));
                        operatorSelectNodes.add(new NodeOption(node, range));
                    }
                }

                FlowSession newSession = this.flowSession.updateSession(node);
                List<IFlowNode> nextNodes = newSession.matchNextNodes();
                this.appendNodes(nextNodes);
            }
        }
    }


}
