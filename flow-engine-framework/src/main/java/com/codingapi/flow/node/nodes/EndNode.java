package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.builder.BaseNodeBuilder;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 结束节点
 */
public class EndNode extends BaseFlowNode {

    public static final String NODE_TYPE = "end";
    public static final String DEFAULT_NAME = "结束节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    @Override
    public boolean isContinueTrigger(FlowSession session) {
        return false;
    }

    public EndNode(String id, String name, List<IFlowAction> actions) {
        super(id, name, actions);
    }

    public EndNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME,new ArrayList<>());
    }

    public static EndNode formMap(Map<String, Object> map) {
        return BaseFlowNode.loadFromMap(map, EndNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder,EndNode> {
        public Builder() {
            super(new EndNode());
        }
    }
}
