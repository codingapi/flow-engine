package com.codingapi.flow.script.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.registry.ScriptRegistry;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.session.FlowSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


/**
 * 异常触发脚本
 */
@AllArgsConstructor
public class ErrorTriggerScript {

    @Getter
    private final String script;

    public ErrorThrow execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        Object value = ScriptRuntimeContext.getInstance().run(script, Object.class, request);
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
        return new ErrorTriggerScript(ScriptRegistry.getInstance().getErrorTriggerScript());
    }

}
