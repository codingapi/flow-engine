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
 * 触发节点脚本
 */
@AllArgsConstructor
public class TriggerScript {

    @Getter
    private final String script;

    public static final String description = """
            触发节点脚本\\n
            脚本说明：即用户自定义处理逻辑。\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为Void类型，即不需要返回数据。\\n
            """;

    public void execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, description, Void.class, GroovyBindObjectBuilder.builder()
                .add("request", request)
                .build());
        ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    public static TriggerScript defaultScript() {
        return new TriggerScript(ScriptRegistryContext.getInstance().getTriggerScript());
    }
}
