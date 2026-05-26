package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyWorkflowRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.springboot.framework.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 人员匹配脚本
 */
@AllArgsConstructor
public class OperatorMatchScript {

    @Getter
    private final String script;

    public boolean execute(GroovyWorkflowRequest request) {
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, Boolean.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
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
