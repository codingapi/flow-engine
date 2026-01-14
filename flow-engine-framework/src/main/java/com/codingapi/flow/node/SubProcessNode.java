package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

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

    public SubProcessNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions);
    }

    private SubProcessNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<SubProcessNode>{
        public Builder() {
            super(new SubProcessNode());
        }
    }
}
