package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点待办标题脚本
 */
@AllArgsConstructor
public class NodeTitleScript {

    @Getter
    private final String script;

    public static final String description = """
            节点待办标题脚本\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为String类型，即为展示的待办标题数据。\\n
            标题数据支持html的语法格式，以适配不同的标题表现形式数据。例如 return "你好，<span style=\"color:read\">XXX</span>你有一个待办数据"
            """;

    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, description, String.class, GroovyBindObjectBuilder.builder()
                .add("request", request)
                .build());
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    /**
     * 默认脚本
     */
    public static NodeTitleScript defaultScript() {
        return new NodeTitleScript(ScriptRegistryContext.getInstance().getNodeTitleScript());
    }
}
