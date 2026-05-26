package com.codingapi.flow.script.runtime;

import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.springboot.framework.script.meta.GroovyMetadata;
import com.codingapi.springboot.framework.script.request.GroovyBindObject;
import com.codingapi.springboot.framework.script.request.GroovyRunningScript;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本传入的$bind对象，对应{@link FlowScriptContext}对象，用于脚本运行时获取相关的服务数据能力。
 * @param <T>
 */
public class ScriptRuntimeRequest {

    @Getter
    private final GroovyRunningScript<?> runningScript;

    public ScriptRuntimeRequest(String script, Class<?> returnType, List<GroovyBindObject> requests) {
        List<GroovyBindObject> bindObjects = new ArrayList<>();
        bindObjects.add(new GroovyBindObject("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())));
        this.runningScript = new GroovyRunningScript<>("run",script,returnType,bindObjects,requests);
        this.runningScript.buildMetadata();
    }

    public GroovyMetadata getMetaData(){
        return this.runningScript.getMetadata();
    }
}
