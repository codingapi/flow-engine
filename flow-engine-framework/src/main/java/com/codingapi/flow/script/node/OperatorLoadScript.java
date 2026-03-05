package com.codingapi.flow.script.node;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.OperatorLoadGroovyRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
        OperatorLoadGroovyRequest request = new OperatorLoadGroovyRequest(session);
        return ScriptRuntimeContext.getInstance().run(script, List.class, request);
    }

    /**
     * 流程创建者
     */
    public static OperatorLoadScript defaultScript() {
        return new OperatorLoadScript(ScriptDefaultConstants.SCRIPT_DEFAULT_OPERATOR_LOAD);
    }

}
