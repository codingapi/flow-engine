package com.codingapi.flow.script.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.registry.ScriptRegistryContext;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.flow.script.runtime.ScriptRuntimeContext;
import com.codingapi.flow.script.runtime.ScriptRuntimeRequest;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.script.request.GroovyBindObjectBuilder;
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

    public static final String description = """
            异常触发脚本\\n
            脚本说明：所谓的异常脚本，是指流程在执行时没有匹配到审批人员时触发的情况。\\n
            传入对象为GroovyScriptRequest对象，返回数据格式为Object类型，实际支持两种数据结构。\\n
            一种是String格式的字符串，对应的节点Id(nodeId)。\\n
            当返回的是字符串类型的节点id时，则会当出现异常时会跳转到对应的节点上去执行审批。\\n
            另一种是List<Long>，即long[]格式，实例格式为return [1,2,3]\\n
            当返回的为List<Long>格式时，代表的是指定审批人员，即当异常时将会将这个流程转交给指定的这些人员来审批。
            """;

    public ErrorThrow execute(FlowSession session) {
        GroovyScriptRequest request = new GroovyScriptRequest(session);
        ScriptRuntimeRequest runtimeRequest = new ScriptRuntimeRequest(script,description, Object.class, GroovyBindObjectBuilder.builder()
                .add("request",request)
                .build());
        Object value = ScriptRuntimeContext.getInstance().execute(runtimeRequest);
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
