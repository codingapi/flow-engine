package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ConditionScript {

    @Getter
    private final String script;


    public static final String description = """
            条件匹配脚本\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为Boolean类型，返回true则为满足条件，执行后续的操作，返回false则不执行后续条件。\\n
            当所有的条件都不满足的时候，默认会执行其他情况，即else的操作逻辑。
            """;


    public boolean execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(
                script,
                description,
                Boolean.class,
                GroovyBindObjectBuilder.builder()
                        .add("request", request)
                        .build()
        );
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    public static ConditionScript defaultScript() {
        return new ConditionScript(ScriptRegistryContext.getInstance().getConditionScript());
    }
}
