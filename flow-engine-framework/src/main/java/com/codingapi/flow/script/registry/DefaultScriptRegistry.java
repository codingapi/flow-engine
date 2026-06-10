package com.codingapi.flow.script.registry;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.factory.FlowGroovyScriptFactory;

/**
 * 默认脚本注册实现
 * <p>
 * 当用户未自定义脚本时，使用此默认实现返回 {@link ScriptDefaultConstants} 中定义的脚本。
 */
public class DefaultScriptRegistry implements IScriptRegistry {


    @Override
    public String getRouterScript() {
        return FlowGroovyScriptFactory.createRouterScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ROUTER).getKey();
    }

    @Override
    public String getNodeTitleScript() {
        return FlowGroovyScriptFactory.createNodeTitleScript(ScriptDefaultConstants.SCRIPT_DEFAULT_NODE_TITLE).getKey();
    }

    @Override
    public String getConditionScript() {
        return FlowGroovyScriptFactory.createConditionScript(ScriptDefaultConstants.SCRIPT_DEFAULT_CONDITION).getKey();
    }

    @Override
    public String getTriggerScript() {
        return FlowGroovyScriptFactory.createTriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_TRIGGER).getKey();
    }

    @Override
    public String getSubProcessScript() {
        return FlowGroovyScriptFactory.createSubProcessScript(ScriptDefaultConstants.SCRIPT_DEFAULT_SUB_PROCESS).getKey();
    }

    @Override
    public String getOperatorLoadScript() {
        return FlowGroovyScriptFactory.createOperatorLoadScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_LOAD).getKey();
    }

    @Override
    public String getOperatorMatchScript() {
        return FlowGroovyScriptFactory.createOperatorMatchScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_MATCH).getKey();
    }

    @Override
    public String getErrorTriggerScript() {
        return FlowGroovyScriptFactory.createErrorTriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ERROR_TRIGGER).getKey();
    }

    @Override
    public String getActionCustomScript() {
        return FlowGroovyScriptFactory.createActionCustomScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_CUSTOM).getKey();
    }

    @Override
    public String getActionRejectScript() {
        return FlowGroovyScriptFactory.createActionRejectScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_REJECT).getKey();
    }

    @Override
    public String getActionDisplayScript() {
       return FlowGroovyScriptFactory.createActionDisplayScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ACTION_DISPLAY).getKey();
    }
}
