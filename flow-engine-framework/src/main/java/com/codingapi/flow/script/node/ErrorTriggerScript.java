package com.codingapi.flow.script.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常触发脚本
 */
@AllArgsConstructor
public class ErrorTriggerScript {


    public static final String SCRIPT_NODE_DEFAULT = """
            // @SCRIPT_TITLE 回退至开始节点 
            def run(request){ 
                return $bind.createErrorThrow(request.getStartNode());
            }
            """;

    public static final String SCRIPT_OPERATOR_DEFAULT = """
            // @SCRIPT_TITLE 指定用户到流程发起者 
            def run(request){ 
                return $bind.createErrorThrow(request.getCreatedOperator());
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
