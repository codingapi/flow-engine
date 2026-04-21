package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义脚本
 */
@AllArgsConstructor
public class CustomScript {

    @Getter
    private final String script;

    /**
     * 返回的动作类型的type
     */
    public String execute(FlowSession session) {
        return ScriptRuntimeContext.getInstance().run(script, String.class, session);
    }

    /**
     * 默认节点脚本
     */
    public static CustomScript defaultScript() {
        return new CustomScript(ScriptRegistryContext.getInstance().getActionCustomScript());
    }

}
