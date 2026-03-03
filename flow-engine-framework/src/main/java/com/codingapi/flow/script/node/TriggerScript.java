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

    public static final String SCRIPT_DEFAULT = """
            // @SCRIPT_TITLE 示例触发节点（打印触发日志） 
            def run(request){ 
                print('hello trigger node.'); 
            }
            """;

    @Getter
    private final String script;

    public void execute(FlowSession request) {
        ScriptRuntimeContext.getInstance().run(script, Void.class, request);
    }

    public static TriggerScript defaultScript() {
        return new TriggerScript(SCRIPT_DEFAULT);
    }
}
