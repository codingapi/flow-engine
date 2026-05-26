package com.codingapi.flow.script.node;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.framework.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 人员加载脚本
 */
@AllArgsConstructor
public class OperatorLoadScript {


    @Getter
    private final String script;

    public List<IFlowOperator> execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, List.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        List<Object> userIds = ScriptRuntimeContext.getInstance().execute(runtimeRequest);
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
