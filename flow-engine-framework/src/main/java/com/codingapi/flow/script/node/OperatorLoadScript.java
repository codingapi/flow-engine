package com.codingapi.flow.script.node;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 人员加载脚本
 */
@AllArgsConstructor
public class OperatorLoadScript {


    @Getter
    @GroovyScript
    private final String script;

    public List<IFlowOperator> execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        List<Object> userIds = GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
        List<Long> operatorIds = new ArrayList<>();
        for (Object userId : userIds) {
            operatorIds.add(Long.parseLong(String.valueOf(userId)));
        }
        return session.getRepositoryHolder().findOperatorByIds(operatorIds);
    }

    /**
     * 流程创建者
     */
    public static OperatorLoadScript defaultScript() {
        return new OperatorLoadScript(ScriptRegistryContext.getInstance().getOperatorLoadScript());
    }

}
