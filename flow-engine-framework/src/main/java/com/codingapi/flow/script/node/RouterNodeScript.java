package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 路由触发脚本
 */
@AllArgsConstructor
public class RouterNodeScript {

    @Getter
    @GroovyScript
    private final String script;


    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
    }

    /**
     * 默认节点脚本
     */
    public static RouterNodeScript defaultScript() {
        return new RouterNodeScript(ScriptRegistryContext.getInstance().getRouterScript());
    }

}
