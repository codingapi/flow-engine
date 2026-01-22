package com.codingapi.flow.script;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorMatchScriptTest {

    @Test
    void execute() {
        IFlowOperator flowOperator = new User(1, "lorne");
        OperatorMatchScript operatorMatchScript = OperatorMatchScript.any();
        assertTrue(operatorMatchScript.execute(flowOperator));
    }
}