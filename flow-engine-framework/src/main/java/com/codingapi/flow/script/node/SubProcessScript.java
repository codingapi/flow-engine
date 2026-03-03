package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {

    public static final String SCRIPT_DEFAULT = """
            // @SCRIPT_TITLE 创建当前流程 
            def run(session){ 
                return session.toCreateRequest() 
            }
            """;

    @Getter
    private final String script;

    public FlowCreateRequest execute(FlowSession request) {
        FlowRecord flowRecord = request.getCurrentRecord();
        FlowCreateRequest flowCreateRequest =  ScriptRuntimeContext.getInstance().run(script, FlowCreateRequest.class, request);
        flowCreateRequest.setParentRecordId(flowRecord.getId());
        return flowCreateRequest;
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(SCRIPT_DEFAULT);
    }
}
