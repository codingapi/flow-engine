package com.codingapi.flow.script.runtime;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.Getter;

public class ScriptRuntimeContext {

    private final static ThreadLocal<GroovyShell> threadLocalShell =
            ThreadLocal.withInitial(GroovyShell::new);

    @Getter
    private final static ScriptRuntimeContext instance = new ScriptRuntimeContext();

    private ScriptRuntimeContext() {
    }

    public <T> T run(String script, Class<T> returnType, Object... args) {
        return this.execute("run", script, returnType, args);
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(String method, String script, Class<T> returnType, Object... args) {
        GroovyShell shell = new GroovyShell();
        try {
            Script runtime = shell.parse(script);
            runtime.setProperty("$bind", FlowScriptContext.getInstance());
            return (T) runtime.invokeMethod(method, args);
        } finally {
            shell = null; // 帮助GC
        }
    }
}
