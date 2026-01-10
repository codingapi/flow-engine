package com.codingapi.flow.node;

/**
 * 审批节点
 */
public class ApprovalNode extends BaseNode{

    public static final String NODE_TYPE = "approval";

    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
