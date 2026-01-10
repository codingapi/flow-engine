package com.codingapi.flow.node;

/**
 * 延迟节点
 */
public class DelayNode extends BaseNode{

    public static final String NODE_TYPE = "delay";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
