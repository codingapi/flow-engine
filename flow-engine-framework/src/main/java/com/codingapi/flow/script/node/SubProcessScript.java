package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.framework.script.request.GroovyBindObjectBuilder;
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
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, FlowCreateRequest.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        FlowCreateRequest flowCreateRequest =  ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        flowCreateRequest.setParentRecordId(flowRecord.getId());
        return flowCreateRequest;
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(ScriptRegistryContext.getInstance().getSubProcessScript());
    }
}
