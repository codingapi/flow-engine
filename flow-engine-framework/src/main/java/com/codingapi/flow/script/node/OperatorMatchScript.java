package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyWorkflowRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 人员匹配脚本
 */
@AllArgsConstructor
public class OperatorMatchScript {

    @Getter
    private final String script;

    public static final String description = """
            人员匹配脚本\\n
            传入对象为GroovyWorkflowRequest类型的request对象，返回数据为Boolean类型，返回true时表明该人拥有发起流程的权限，否则反之。
            """;

    public boolean execute(GroovyWorkflowRequest request) {
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,
                description,
                Boolean.class,
                GroovyBindObjectBuilder.builder()
                        .add("request", request)
                        .build());
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    /**
     * 任意人
     */
    public static OperatorMatchScript any() {
        return new OperatorMatchScript(ScriptRegistryContext.getInstance().getOperatorMatchScript());
    }
}
