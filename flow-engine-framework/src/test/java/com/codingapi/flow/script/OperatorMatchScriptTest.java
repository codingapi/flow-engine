package com.codingapi.flow.script;

import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperatorMatchScriptTest {

    @Test
    void execute() {
        IFlowOperator flowOperator = new User(1,"lorne");
        OperatorMatchScript operatorMatchScript = OperatorMatchScript.any();
        assertTrue(operatorMatchScript.execute(flowOperator));
    }
}