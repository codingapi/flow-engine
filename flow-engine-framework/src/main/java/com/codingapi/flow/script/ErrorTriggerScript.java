package com.codingapi.flow.script;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ErrorTriggerScript {


    public static final String SCRIPT_NODE_DEFAULT = """
            def run(request){
                return com.codingapi.flow.error.ErrorThrow.builder()
                                                          .node(request.getStartNode())
                                                          .build();
            }
            """;

    public static final String SCRIPT_OPERATOR_DEFAULT = """
            def run(request){
                return com.codingapi.flow.error.ErrorThrow.builder()
                                                          .operators(request.getCreatedOperator())
                                                          .build();
            }
            """;

    @Getter
    private final String script;

    public ErrorThrow execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, ErrorThrow.class, request);
    }

    /**
     * 默认节点脚本
     */
    public static ErrorTriggerScript defaultNodeScript() {
        return new ErrorTriggerScript(SCRIPT_NODE_DEFAULT);
    }

    /**
     * 默认审批人配置脚本
     */
    public static ErrorTriggerScript defaultOperatorScript() {
        return new ErrorTriggerScript(SCRIPT_OPERATOR_DEFAULT);
    }
}
