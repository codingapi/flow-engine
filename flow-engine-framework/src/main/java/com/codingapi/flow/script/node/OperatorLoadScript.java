package com.codingapi.flow.script.node;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
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

    public static final String description = """
            匹配人员列表脚本\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为List<Long>类型，即long[]数组格式，每一个long的id数据为一个人员的id，实例格式为[1,2,3]\\n
            返回的人员id将会作为匹配人员信息，作为流程的审批者。
            """;

    public List<IFlowOperator> execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,description, List.class, GroovyBindObjectBuilder.builder()
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
