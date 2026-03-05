package com.codingapi.flow.script.node;

import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.request.TitleGroovyRequest;
import com.codingapi.flow.session.FlowSession;
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
        TitleGroovyRequest request = new TitleGroovyRequest(session);
        return ScriptRuntimeContext.getInstance().run(script, String.class, request);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(ScriptDefaultConstants.SCRIPT_DEFAULT_NODE_TITLE);
    }
}
