package com.codingapi.flow.script.registry;

import com.codingapi.flow.script.ScriptDefaultConstants;

/**
 * 默认脚本注册实现
 * <p>
 * 当用户未自定义脚本时，使用此默认实现返回 {@link ScriptDefaultConstants} 中定义的脚本。
 */
public class DefaultScriptRegistry implements IScriptRegistry {

    @Override
    public String getRouterScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_ROUTER;
    }

    @Override
    public String getNodeTitleScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_NODE_TITLE;
    }

    @Override
    public String getConditionScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_CONDITION;
    }

    @Override
    public String getTriggerScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_TRIGGER;
    }

    @Override
    public String getSubProcessScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_SUB_PROCESS;
    }

    @Override
    public String getOperatorLoadScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_LOAD;
    }

    @Override
    public String getOperatorMatchScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_MATCH;
    }

    @Override
    public String getErrorTriggerScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_ERROR_TRIGGER;
    }

    @Override
    public String getActionCustomScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_CUSTOM;
    }

    @Override
    public String getActionRejectScript() {
        return ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_REJECT;
    }
}
