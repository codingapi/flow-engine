package com.codingapi.flow.script.node;

import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常触发脚本
 */
@AllArgsConstructor
public class RouterNodeScript {


    public static final String SCRIPT_NODE_DEFAULT = """
            def run(request){
                return request.getStartNode().getId();
            }
            """;


    @Getter
    private final String script;

    public String execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, String.class, request);
    }

    /**
     * 默认节点脚本
     */
    public static RouterNodeScript defaultNodeScript() {
        return new RouterNodeScript(SCRIPT_NODE_DEFAULT);
    }

}
