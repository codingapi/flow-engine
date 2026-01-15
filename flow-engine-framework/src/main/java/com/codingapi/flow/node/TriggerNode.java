package com.codingapi.flow.node;

import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.script.ErrorTriggerScript;
import com.codingapi.flow.script.NodeTitleScript;
import com.codingapi.flow.script.OperatorLoadScript;
import com.codingapi.flow.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 触发节点
 */
public class TriggerNode extends BaseNode{

    public static final String NODE_TYPE = "trigger";
    public static final String DEFAULT_NAME = "触发节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }

    public TriggerNode(String id, String name, String view, OperatorLoadScript operatorScript, NodeTitleScript nodeTitleScript, ErrorTriggerScript errorTriggerScript, List<FormFieldPermission> formFieldsPermissions) {
        super(id, name, view, operatorScript, nodeTitleScript, errorTriggerScript, formFieldsPermissions);
    }

    protected TriggerNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME, DEFAULT_VIEW, OperatorLoadScript.creator(), NodeTitleScript.defaultScript(), ErrorTriggerScript.defaultNodeScript(), new ArrayList<>());
    }

    public static TriggerNode formMap(Map<String, Object> map) {
        return BaseNode.formMap(map, TriggerNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<TriggerNode>{
        public Builder() {
            super(new TriggerNode());
        }
    }
}
