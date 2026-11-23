package com.codingapi.flow.action.actions.service;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.response.NodeOption;
import com.codingapi.flow.service.impl.OperatorAssignmentService;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class InitiatorSelectNodeService {

    @Getter
    private final List<NodeOption> operatorSelectNodes = new ArrayList<>();
    private final FlowSession flowSession;

    public InitiatorSelectNodeService(FlowSession flowSession) {
        this.flowSession = flowSession;
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
                    // 兼容存量 map、含身份的新字段，以及发起时已落库的分配（issue #288 衍生 / #292）
                    if (!OperatorAssignmentService.hasAssignment(this.flowSession, node.getId())) {
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
