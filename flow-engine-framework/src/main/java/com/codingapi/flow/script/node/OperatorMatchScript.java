package com.codingapi.flow.script.node;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyWorkflowRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 人员匹配脚本
 */
@AllArgsConstructor
public class OperatorMatchScript {

    @Getter
    @GroovyScript
    private final String script;

    public boolean execute(GroovyWorkflowRequest request) {
        return GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
    }

    /**
     * 任意人
     */
    public static OperatorMatchScript any() {
        return new OperatorMatchScript(ScriptRegistryContext.getInstance().getOperatorMatchScript());
    }
}
