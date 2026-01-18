package com.codingapi.flow.script.runtime;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;

public class ScriptRuntimeContext {

    private final static GroovyShell groovyShell = new GroovyShell();

    @Getter
    private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();

    private ScriptRuntimeContext() {
    }

    public <T> T run(String script, Class<T> returnType, Object... args) {
        return this.execute("run", script, returnType, args);
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(String method, String script, Class<T> returnType, Object... args) {
        Script runtime = groovyShell.parse(script);
        runtime.setProperty("$bind", FlowScriptContext.getInstance());
        return (T) runtime.invokeMethod(method, args);
    }
}
