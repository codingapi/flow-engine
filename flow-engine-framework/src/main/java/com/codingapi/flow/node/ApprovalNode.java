package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public ApprovalNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions);
    }

    public ApprovalNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>());
    }


    public static ApprovalNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, ApprovalNode.class);
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<ApprovalNode> {

        public Builder() {
            super(new ApprovalNode());
        }
    }
}
