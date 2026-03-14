package com.codingapi.flow.script.node;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
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

    @SuppressWarnings("unchecked")
    public List<IFlowOperator> execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        List<Object> userIds = ScriptRuntimeContext.getInstance().run(script, List.class, request);
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
        return new OperatorLoadScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_LOAD);
    }

}
