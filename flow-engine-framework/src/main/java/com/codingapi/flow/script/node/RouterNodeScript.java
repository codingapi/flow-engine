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
 * 路由触发脚本
 */
@AllArgsConstructor
public class RouterNodeScript {

    @Getter
    private final String script;

    public static final String description = """
            路由触发脚本\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为String类型\\n
            返回的是为跳转的流程节点id，即nodeId。
            """;

    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, description, String.class, GroovyBindObjectBuilder.builder()
                .add("request", request)
                .build());
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    /**
     * 默认节点脚本
     */
    public static RouterNodeScript defaultScript() {
        return new RouterNodeScript(ScriptRegistryContext.getInstance().getRouterScript());
    }

}
