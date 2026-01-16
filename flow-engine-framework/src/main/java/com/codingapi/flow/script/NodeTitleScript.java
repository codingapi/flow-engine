package com.codingapi.flow.script;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点待办标题脚本
 */
@AllArgsConstructor
public class NodeTitleScript {

    public static final String SCRIPT_DEFAULT = "def run(request){return '你有一条待办'}";

    @Getter
    private final String script;

    public String execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, String.class, request);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(SCRIPT_DEFAULT);
    }
}
