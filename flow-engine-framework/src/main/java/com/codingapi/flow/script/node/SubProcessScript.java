package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.ScriptDefaultConstants;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {


    @Getter
    private final String script;

    public FlowCreateRequest execute(FlowSession session) {
        FlowRecord flowRecord = session.getCurrentRecord();
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        FlowCreateRequest flowCreateRequest =  ScriptRuntimeContext.getInstance().run(script, FlowCreateRequest.class, request);
        flowCreateRequest.setParentRecordId(flowRecord.getId());
        return flowCreateRequest;
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(ScriptDefaultConstants.SCRIPT_DEFAULT_SUB_PROCESS);
    }
}
