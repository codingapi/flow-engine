package com.codingapi.flow.node;

/**
 * 延迟节点
 */
public class DelayNode extends BaseNode{

    public static final String NODE_TYPE = "delay";
    public static final String DEFAULT_NAME = "延迟节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public DelayNode() {
        super(DEFAULT_NAME);
    }

    public DelayNode(String name) {
        super(name);
    }
}
