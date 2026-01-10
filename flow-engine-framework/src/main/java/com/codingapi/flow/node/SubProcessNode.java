package com.codingapi.flow.node;

/**
 * 子流程
 */
public class SubProcessNode extends BaseNode{

    public static final String NODE_TYPE = "sub_process";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
