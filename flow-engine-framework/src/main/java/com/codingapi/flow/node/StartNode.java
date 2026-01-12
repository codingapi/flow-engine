package com.codingapi.flow.node;

/**
 * 开始节点
 */
public class StartNode extends BaseNode  {

    public static final String NODE_TYPE = "start";
    public static final String DEFAULT_NAME = "开始节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public StartNode() {
        super(DEFAULT_NAME);
    }

    public StartNode(String name) {
        super(name);
    }
}
