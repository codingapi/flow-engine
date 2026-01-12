package com.codingapi.flow.node;

/**
 * 审批节点
 */
public class ApprovalNode extends BaseNode {

    public static final String NODE_TYPE = "approval";
    public static final String DEFAULT_NAME = "审批节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public ApprovalNode() {
        super(DEFAULT_NAME);
    }

    public ApprovalNode(String name) {
        super(name);
    }
}
