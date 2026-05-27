package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义脚本
 */
@AllArgsConstructor
public class CustomScript {

    @Getter
    private final String script;

    public static final String description = """
            自定义脚本\\n
            脚本说明：即当点击自定义按钮时的操作逻辑，在完成自定义的业务之后，需要再联动一个动作。\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为String类型，为操作类型，数据分类有SAVE、PASS、REJECT、ADD_AUDIT、DELEGATE、RETURN、TRANSFER。\\n
            对应的业务含义分为为：保存、通过、拒绝、加签、委派、退回、转办。\\n
            """;
    /**
     * 返回的动作类型的type
     */
    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,description, String.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        return ScriptRuntimeContext.getInstance().execute(runtimeRequest);
    }

    /**
     * 默认节点脚本
     */
    public static CustomScript defaultScript() {
        return new CustomScript(ScriptRegistryContext.getInstance().getActionCustomScript());
    }

}
