package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.FlowScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 子流程结果确认脚本。
 */
@AllArgsConstructor
public class SubProcessResultScript {

    @Getter
    @GroovyScript
    private final String script;

    public boolean execute(FlowSession session) {
        Object value = FlowScriptRuntimeContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())),
                        new GroovyScriptRequest(session));
        return Boolean.TRUE.equals(value);
    }

    public static SubProcessResultScript defaultScript() {
        return new SubProcessResultScript(ScriptRegistryContext.getInstance().getSubProcessResultScript());
    }
}
