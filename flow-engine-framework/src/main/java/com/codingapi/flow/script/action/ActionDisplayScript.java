package com.codingapi.flow.script.action;

import java.util.Map;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ActionDisplayScript {

    @Getter
    @GroovyScript
    private final String script;

    /**
     * 返回的动作类型的type
     */
    public boolean execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
    }

    /**
     * 默认节点脚本
     */
    public static ActionDisplayScript defaultScript() {
        return new ActionDisplayScript(ScriptRegistryContext.getInstance().getActionDisplayScript());
    }
}
