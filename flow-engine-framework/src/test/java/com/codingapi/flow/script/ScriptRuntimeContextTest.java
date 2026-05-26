package com.codingapi.flow.script;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.user.User;
import com.codingapi.springboot.framework.script.meta.GroovyMetadata;
import com.codingapi.springboot.framework.script.request.GroovyBindObjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRuntimeContextTest {

    private final UserGateway gateway = new UserGateway();


    @Test
    void execute1() {
        String script = "def run(abc){return 1}";
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,Integer.class, GroovyBindObjectBuilder.builder()
                .add("abc",1)
                .build());

        GroovyMetadata metadata = runtimeRequest.getMetaData();
        System.out.println(metadata);

        int value = ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        assertEquals(1, value);
    }

    @Test
    void execute2() {
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);

        User user = new User(1, "codingapi");
        gateway.save(user);
        String script = "def run(abc){return $bind.getOperatorById(1)}";

        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,IFlowOperator.class, GroovyBindObjectBuilder.builder()
                .add("abc",1)
                .build());

        IFlowOperator target = ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        assertEquals(target, user);
    }

    @Test
    void testAutoCleanup() {
        // 设置较小的缓存阈值
        ScriptRuntimeContext.getInstance().setMaxLockCacheSize(10);

        // 执行超过阈值的脚本数量
        Set<String> scripts = new HashSet<>();
        for (int i = 0; i < 15; i++) {
            String script = "def run(abc){return " + i + "}";
            scripts.add(script);

            ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,Integer.class, GroovyBindObjectBuilder.builder()
                    .add("abc",i)
                    .build());

            ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        }

    }



    @Test
    void testClearCache() {
        // 执行一些脚本以填充缓存
        for (int i = 0; i < 5; i++) {
            String script = "def run(abc){return " + i + "}";

            ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,Integer.class, GroovyBindObjectBuilder.builder()
                    .add("abc",i)
                    .build());

            ScriptRuntimeContext.getInstance().execute(runtimeRequest);
        }



    }
}