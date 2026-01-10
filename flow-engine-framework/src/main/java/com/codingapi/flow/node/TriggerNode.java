package com.codingapi.flow.node;

/**
 * 触发节点
 */
public class TriggerNode extends BaseNode{

    public static final String NODE_TYPE = "trigger";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
