package com.codingapi.flow.node;

/**
 * 触发节点
 */
public class TriggerNode extends BaseNode{

    public static final String NODE_TYPE = "trigger";
    public static final String DEFAULT_NAME = "触发节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public TriggerNode() {
        super(DEFAULT_NAME);
    }

    public TriggerNode(String name) {
        super(name);
    }
}
