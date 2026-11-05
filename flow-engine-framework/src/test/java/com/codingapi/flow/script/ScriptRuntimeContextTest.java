package com.codingapi.flow.script;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.user.User;
import com.codingapi.springboot.script.GroovyScript;
import com.codingapi.springboot.script.meta.GroovyMetadata;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptRuntimeContextTest {

    private final UserGateway gateway = new UserGateway();


    @Test
    void execute1() {
        String script = "def run(abc){return 1}";
        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        GroovyScript groovyScript = GroovyScript
                .builder(key)
                .script(script)
                .method("run")
                .returnType(Integer.class)
                .requests( Map.of("abc", Integer.class))
                .build();

        GroovyMetadata metadata = groovyScript.toMetadata();
        System.out.println(metadata);

        int value = groovyScript.invoke(1);
        assertEquals(1, value);
    }

    @Test
    void execute2() {
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);

        User user = new User(1, "codingapi");
        gateway.save(user);
        String script = "def run(abc){return $bind.getOperatorById(1)}";

        String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
        GroovyScript groovyScript = GroovyScript
                .builder(key)
                .script(script)
                .method("run")
                .returnType(IFlowOperator.class)
                .binds(Map.of("$bind", GroovyScriptBind.class))
                .requests( Map.of("abc", Integer.class))
                .build();

        // $bind 必须显式作为绑定对象传入：脚本运行时按脚本文本缓存 Script 实例，
        // 绑定属性挂在缓存实例上，若依赖其他用例执行后残留的绑定，测试结果将受执行顺序影响
        GroovyScriptBind bind = new GroovyScriptBind(FlowScriptContext.getInstance());
        IFlowOperator target = groovyScript.invoke(Map.of("$bind", bind), 1);
        assertEquals(user, target);
    }

    @Test
    void testAutoCleanup() {
        // 设置较小的缓存阈值

        // 执行超过阈值的脚本数量
        Set<String> scripts = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            String script = "def run(abc){return " + i + "}";
            scripts.add(script);

            String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
            GroovyScript groovyScript = GroovyScript
                    .builder(key)
                    .script(script)
                    .method("run")
                    .returnType(Integer.class)
                    .requests( Map.of("abc", Integer.class))
                    .build();

            groovyScript.invoke(i);
        }

    }



    @Test
    void testClearCache() {
        // 执行一些脚本以填充缓存
        for (int i = 0; i < 5; i++) {
            String script = "def run(abc){return " + i + "}";

            String key = FlowIDGeneratorGatewayContext.getInstance().generateFlowScriptKey();
            GroovyScript groovyScript = GroovyScript
                    .builder(key)
                    .script(script)
                    .method("run")
                    .returnType(Integer.class)
                    .requests( Map.of("abc", Integer.class))
                    .build();

            groovyScript.invoke(i);
        }



    }
}