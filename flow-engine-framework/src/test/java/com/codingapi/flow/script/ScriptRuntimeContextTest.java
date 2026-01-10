package com.codingapi.flow.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeContextTest {

    @Test
    void execute() {
        String script = "def run(abc){return 1}";
        int value = ScriptRuntimeContext.getInstance().run(script,Integer.class, 1);
        assertEquals(1,value);
    }
}