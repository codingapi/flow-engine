package com.codingapi.flow.script;

import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.user.IFlowOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 人员加载脚本
 */
@AllArgsConstructor
public class OperatorLoadScript {

    public static final String SCRIPT_CREATOR = "def run(request){return [request.getCreatedOperator()]}";

    @Getter
    private final String script;

    @SuppressWarnings("unchecked")
    public List<IFlowOperator> execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, List.class,request);
    }

    /**
     * 流程创建者
     */
    public static OperatorLoadScript creator() {
        return new OperatorLoadScript(SCRIPT_CREATOR);
    }

}
