package com.codingapi.flow.script.action;

import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.utils.GroovyScriptUtils;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.annotation.GroovyScript;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 自定义脚本
 */
@AllArgsConstructor
public class ActionCustomScript {

    @Getter
    @GroovyScript
    private final String script;


    /**
     * 获取触发类型
     */
    public String getTriggerType(){
        if(script!=null){
            String scriptData = GroovyScriptCacheContext.getInstance()
                    .getGroovyScript(script)
                    .getScript();

            String returnData = GroovyScriptUtils.getReturnScript(scriptData);
            if(StringUtils.hasText(returnData)){
                return returnData.replace("\"","").replace("'","");
            }
        }
        return null;
    }

    /**
     * 返回的动作类型的type
     */
    public String execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        return GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
    }

    /**
     * 默认节点脚本
     */
    public static ActionCustomScript defaultScript() {
        return new ActionCustomScript(ScriptRegistryContext.getInstance().getActionCustomScript());
    }

}
