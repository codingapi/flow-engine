package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {

    public static final String SCRIPT_DEFAULT = "def run(session){ return session.toCreateRequest() }";

    @Getter
    private final String script;

    public FlowCreateRequest execute(FlowSession request) {
        return ScriptRuntimeContext.getInstance().run(script, FlowCreateRequest.class, request);
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(SCRIPT_DEFAULT);
    }
}
