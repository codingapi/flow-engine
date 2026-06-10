package com.codingapi.flow.script.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroovyScriptUtilsTest {

    @Test
    void getReturnScript1() {
        String groovy = """
                def run(request){
                    return "PASS"
                }
                """;
        String result = GroovyScriptUtils.getReturnScript(groovy);
        assertEquals("\"PASS\"",result);
    }

    @Test
    void getReturnScript2() {
        String groovy = """
                def run(request){
                    return ["PASS","PASS"];
                }
                """;
        String result = GroovyScriptUtils.getReturnScript(groovy);
        assertEquals("[\"PASS\",\"PASS\"]",result);
    }

    @Test
    void getReturnScript3() {
        String groovy = """
                def run(request){return ["PASS","PASS"]}
                """;
        String result = GroovyScriptUtils.getReturnScript(groovy);
        assertEquals("[\"PASS\",\"PASS\"]",result);
    }

    @Test
    void getReturnScript4() {
        String groovy = """
                def run(request){return true;}
                """;
        String result = GroovyScriptUtils.getReturnScript(groovy);
        assertEquals("true",result);
    }
}