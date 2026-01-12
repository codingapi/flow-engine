package com.codingapi.flow.node;

/**
 * 结束节点
 */
public class EndNode extends BaseNode  {

    public static final String NODE_TYPE = "end";
    public static final String DEFAULT_NAME = "结束节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public EndNode() {
        super(DEFAULT_NAME);
    }

    public EndNode(String name) {
        super(name);
    }
}
