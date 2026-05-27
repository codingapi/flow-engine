package com.codingapi.flow.script.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptBind;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.cache.GroovyScriptCacheContext;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 异常触发脚本
 */
@AllArgsConstructor
public class ErrorTriggerScript {

    @Getter
    private final String script;

    public ErrorThrow execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        Object value = GroovyScriptCacheContext.getInstance()
                .getGroovyScript(script)
                .invoke(Map.of("$bind", new GroovyScriptBind(FlowScriptContext.getInstance())), request);
        if(value instanceof String){
            String nodeId = (String) value;
            ErrorThrow errorThrow = new ErrorThrow();
            errorThrow.setNode(session.getNode(nodeId));
            return errorThrow;
        }
        if(value instanceof List){
            List<Object> userIds =(List<Object>) value;
            List<Long> operatorIds = new ArrayList<>();
            for(Object userId:userIds){
                operatorIds.add(Long.parseLong(String.valueOf(userId)));
            }
            ErrorThrow errorThrow = new ErrorThrow();
            errorThrow.setOperators(session.getRepositoryHolder().findOperatorByIds(operatorIds));
            return errorThrow;
        }

        long userId = Long.parseLong(String.valueOf(value));
        ErrorThrow errorThrow = new ErrorThrow();
        List<IFlowOperator> operatorList = new ArrayList<>();
        operatorList.add(session.getRepositoryHolder().getOperatorById(userId));
        errorThrow.setOperators(operatorList);
        return errorThrow;

    }

    /**
     * 默认节点脚本
     */
    public static ErrorTriggerScript defaultScript() {
        return new ErrorTriggerScript(ScriptRegistryContext.getInstance().getErrorTriggerScript());
    }

}
