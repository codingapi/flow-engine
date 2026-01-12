package com.codingapi.flow.node;

/**
 * 子流程
 */
public class SubProcessNode extends BaseNode {

    public static final String NODE_TYPE = "sub_process";
    public static final String DEFAULT_NAME = "子流程";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public SubProcessNode() {
        super(DEFAULT_NAME);
    }

    public SubProcessNode(String name) {
        super(name);
    }
}
