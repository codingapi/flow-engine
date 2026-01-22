package com.codingapi.flow.script.node;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 触发节点脚本
 */
@AllArgsConstructor
public class TriggerScript {

    public static final String SCRIPT_DEFAULT = "def run(session){ print('hello trigger node.') }";

    @Getter
    private final String script;

    public void execute(FlowSession request) {
        ScriptRuntimeContext.getInstance().run(script, Void.class, request);
    }

    public static TriggerScript defaultScript() {
        return new TriggerScript(SCRIPT_DEFAULT);
    }
}
