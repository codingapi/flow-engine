package com.codingapi.flow.node;

/**
 * 办理节点
 */
public class HandleNode extends BaseNode{

    public static final String NODE_TYPE = "handle";
    public static final String DEFAULT_NAME = "办理节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public HandleNode() {
        super(DEFAULT_NAME);
    }

    public HandleNode(String name) {
        super(name);
    }
}
