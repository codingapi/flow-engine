package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 节点待办标题脚本
 */
@AllArgsConstructor
public class NodeTitleScript {

    @Getter
    private final String script;


    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(ScriptRegistryContext.getInstance().getNodeTitleScript());
    }
}
