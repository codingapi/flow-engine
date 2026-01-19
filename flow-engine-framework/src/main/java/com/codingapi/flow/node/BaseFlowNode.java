package com.codingapi.flow.node;

import com.codingapi.flow.node.branch.RouterBranchNode;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
public abstract class BaseFlowNode implements IFlowNode {

    /**
     * 节点id
     */
    @Getter
    @Setter
    protected String id;
    /**
     * 节点名称
     */
    @Getter
    @Setter
    protected String name;


    @Override
    public List<IFlowNode> nextNodes(FlowSession session) {
        Workflow workflow = session.getWorkflow();
        if(this instanceof RouterBranchNode routerBranchNode){
            return routerBranchNode.matchRouters(session);
        }else {
            return workflow.edgeNext(this);
        }
    }
}
