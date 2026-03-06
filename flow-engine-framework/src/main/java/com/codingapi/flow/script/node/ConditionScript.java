package com.codingapi.flow.script.node;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ConditionScript {

    @Getter
    private final String script;

    public boolean execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return ScriptRuntimeContext.getInstance().run(script, Boolean.class, request);
    }

    public static ConditionScript defaultScript() {
        return new ConditionScript(ScriptDefaultConstants.SCRIPT_DEFAULT_CONDITION);
    }
}
