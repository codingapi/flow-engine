package com.codingapi.flow.script.node;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.flow.script.runtime.FlowScriptRuntimeContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 子流程任务脚本
 */
@AllArgsConstructor
public class SubProcessScript {


    @Getter
    @GroovyScript
    private final String script;

    public List<FlowCreateRequest> execute(FlowSession session) {
        FlowRecord flowRecord = session.getCurrentRecord();
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        Object value = FlowScriptRuntimeContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
        List<FlowCreateRequest> flowCreateRequests;
        if (value instanceof FlowCreateRequest flowCreateRequest) {
            flowCreateRequests = List.of(flowCreateRequest);
        } else if (value instanceof Collection<?> collection) {
            flowCreateRequests = collection.stream()
                    .map(item -> {
                        if (!(item instanceof FlowCreateRequest flowCreateRequest)) {
                            throw new IllegalArgumentException("sub process script must return FlowCreateRequest list");
                        }
                        return flowCreateRequest;
                    })
                    .toList();
        } else {
            throw new IllegalArgumentException("sub process script must return FlowCreateRequest or list");
        }
        if (flowCreateRequests.isEmpty()) {
            throw new IllegalArgumentException("sub process script result cannot be empty");
        }
        flowCreateRequests.forEach(requestItem -> requestItem.setParentRecordId(flowRecord.getId()));
        return flowCreateRequests;
    }

    public static SubProcessScript defaultScript() {
        return new SubProcessScript(ScriptRegistryContext.getInstance().getSubProcessScript());
    }
}
