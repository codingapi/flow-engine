package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {


    @Getter
    private final String script;


    public static final String description = """
            子流程任务脚本\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为FlowCreateRequest类型\\n
            概对象是流程发起的参数对象，通过构建该对象控制子流程触发的流程节点。
            """;

    public FlowCreateRequest execute(FlowSession session) {
        FlowRecord flowRecord = session.getCurrentRecord();
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script, description,FlowCreateRequest.class, GroovyBindObjectBuilder.builder()
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
