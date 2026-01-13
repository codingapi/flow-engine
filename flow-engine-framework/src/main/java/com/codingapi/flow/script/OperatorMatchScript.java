package com.codingapi.flow.script;

import com.codingapi.flow.user.IFlowOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 人员匹配脚本
 */
@AllArgsConstructor
public class OperatorMatchScript {

    public static final String SCRIPT_ANY = "def run(operator){return true}";

    @Getter
    private final String script;

    public boolean execute(IFlowOperator current) {
        return ScriptRuntimeContext.getInstance().run(script, Boolean.class, current);
    }

    /**
     * 任意人
     */
    public static OperatorMatchScript any() {
        return new OperatorMatchScript(SCRIPT_ANY);
    }
}
