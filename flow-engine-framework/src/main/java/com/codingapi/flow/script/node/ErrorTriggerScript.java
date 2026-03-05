package com.codingapi.flow.script.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 异常触发脚本
 */
@AllArgsConstructor
public class ErrorTriggerScript {

    @Getter
    private final String script;

    public ErrorThrow execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, ErrorThrow.class, request);
    }

    /**
     * 默认节点脚本
     */
    public static ErrorTriggerScript defaultScript() {
        return new ErrorTriggerScript(ScriptDefaultConstants.SCRIPT_DEFAULT_ERROR_TRIGGER);
    }

}
