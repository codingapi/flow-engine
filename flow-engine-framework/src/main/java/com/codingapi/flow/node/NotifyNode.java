package com.codingapi.flow.node;

/**
 * 抄送节点
 */
public class NotifyNode extends BaseNode{

    public static final String NODE_TYPE = "notify";
    public static final String DEFAULT_NAME = "抄送节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public NotifyNode() {
        super(DEFAULT_NAME);
    }

    public NotifyNode(String name) {
        super(name);
    }
}
