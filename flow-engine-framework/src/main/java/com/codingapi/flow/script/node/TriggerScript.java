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
 * 触发节点脚本
 */
@AllArgsConstructor
public class TriggerScript {

    @Getter
    private final String script;

    public void execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, Void.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    public static TriggerScript defaultScript() {
        return new TriggerScript(ScriptRegistryContext.getInstance().getTriggerScript());
    }
}
