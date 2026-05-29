package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * 子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {


    @Getter
    @GroovyScript
    private final String script;

    public FlowCreateRequest execute(FlowSession session) {
        FlowRecord flowRecord = session.getCurrentRecord();
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        FlowCreateRequest flowCreateRequest = GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
        flowCreateRequest.setParentRecordId(flowRecord.getId());
        return flowCreateRequest;
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(ScriptRegistryContext.getInstance().getSubProcessScript());
    }
}
