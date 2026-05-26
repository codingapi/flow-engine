package com.codingapi.flow.script.node;

import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.framework.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点待办标题脚本
 */
@AllArgsConstructor
public class NodeTitleScript {

    @Getter
    private final String script;

    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, String.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(ScriptRegistryContext.getInstance().getNodeTitleScript());
    }
}
