package com.codingapi.flow.script.node;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路由触发脚本
 */
@AllArgsConstructor
public class RouterNodeScript {

    @Getter
    private final String script;

    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return ScriptRuntimeContext.getInstance().run(script, String.class, request);
    }

    /**
     * 默认节点脚本
     */
    public static RouterNodeScript defaultScript() {
        return new RouterNodeScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ROUTER);
    }

}
