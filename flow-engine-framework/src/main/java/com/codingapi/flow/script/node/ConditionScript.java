package com.codingapi.flow.script.node;

import com.codingapi.flow.script.request.ConditionGroovyRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ConditionScript {

    public static final String SCRIPT_DEFAULT = """
            // @SCRIPT_TITLE 默认条件（允许执行）
            def run(request){
                return true;
            }
            """;

    @Getter
    private final String script;

    public boolean execute(FlowSession session) {
        ConditionGroovyRequest request = new ConditionGroovyRequest(session);
        return ScriptRuntimeContext.getInstance().run(script, Boolean.class, request);
    }

    public static ConditionScript defaultScript() {
        return new ConditionScript(SCRIPT_DEFAULT);
    }
}
