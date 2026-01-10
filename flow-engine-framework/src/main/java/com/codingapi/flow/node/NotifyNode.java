package com.codingapi.flow.node;

/**
 * 抄送节点
 */
public class NotifyNode extends BaseNode{

    public static final String NODE_TYPE = "notify";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
