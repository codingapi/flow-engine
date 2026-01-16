package com.codingapi.flow.script;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptRuntimeContextTest {

    private final UserGateway gateway = new UserGateway();

    @Test
    void execute1() {
        String script = "def run(abc){return 1}";
        int value = ScriptRuntimeContext.getInstance().run(script, Integer.class, 1);
        assertEquals(1, value);
    }

    @Test
    void execute2() {
        GatewayContext.getInstance().setFlowOperatorGateway(gateway);

        User user = new User(1, "codingapi");
        gateway.save(user);
        String script = "def run(abc){return $bind.getOperatorById(1)}";
        IFlowOperator target = ScriptRuntimeContext.getInstance().run(script, IFlowOperator.class, 1);
        assertEquals(target, user);
    }
}