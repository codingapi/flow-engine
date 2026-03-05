package com.codingapi.flow.script.node;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
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
        ScriptRuntimeContext.getInstance().run(script, Void.class, request);
    }

    public static TriggerScript defaultScript() {
        return new TriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_TRIGGER);
    }
}
