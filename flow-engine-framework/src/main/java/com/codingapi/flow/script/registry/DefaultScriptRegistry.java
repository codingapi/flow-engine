package com.codingapi.flow.script.registry;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.factory.GroovyScriptFactory;

/**
 * 默认脚本注册实现
 * <p>
 * 当用户未自定义脚本时，使用此默认实现返回 {@link ScriptDefaultConstants} 中定义的脚本。
 */
public class DefaultScriptRegistry implements IScriptRegistry {


    @Override
    public String getRouterScript() {
        return GroovyScriptFactory.getRouterScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ROUTER).getKey();
    }

    @Override
    public String getNodeTitleScript() {
        return GroovyScriptFactory.getNodeTitleScript(ScriptDefaultConstants.SCRIPT_DEFAULT_NODE_TITLE).getKey();
    }

    @Override
    public String getConditionScript() {
        return GroovyScriptFactory.getConditionScript(ScriptDefaultConstants.SCRIPT_DEFAULT_CONDITION).getKey();
    }

    @Override
    public String getTriggerScript() {
        return GroovyScriptFactory.getTriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_TRIGGER).getKey();
    }

    @Override
    public String getSubProcessScript() {
        return GroovyScriptFactory.getSubProcessScript(ScriptDefaultConstants.SCRIPT_DEFAULT_SUB_PROCESS).getKey();
    }

    @Override
    public String getOperatorLoadScript() {
        return GroovyScriptFactory.getOperatorLoadScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_LOAD).getKey();
    }

    @Override
    public String getOperatorMatchScript() {
        return GroovyScriptFactory.getOperatorMatchScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_MATCH).getKey();
    }

    @Override
    public String getErrorTriggerScript() {
        return GroovyScriptFactory.getErrorTriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ERROR_TRIGGER).getKey();
    }

    @Override
    public String getActionCustomScript() {
        return GroovyScriptFactory.getActionCustomScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_CUSTOM).getKey();
    }

    @Override
    public String getActionRejectScript() {
        return GroovyScriptFactory.getActionRejectScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_REJECT).getKey();
    }
}
