package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.springboot.script.annotation.ScriptFunction;
import com.codingapi.springboot.script.annotation.ScriptParameter;
import com.codingapi.springboot.script.annotation.ScriptType;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 流程groovy脚本绑定对象 $bind
 * def run(request){
 * $bind.getRecordById(1);
 * }
 */
@ScriptType(description = "流程groovy脚本绑定对象")
@AllArgsConstructor
public class GroovyScriptBind {

    private final FlowScriptContext context;

    @ScriptFunction(
            name = "getBean",
            description = "获取spring bean对象")
    public <T> T getBean(@ScriptParameter(description = "Class类型") Class<T> clazz) {
        return context.getBean(clazz);
    }

    @ScriptFunction(
            name = "getBean",
            description = "获取spring bean对象"
    )
    public <T> T getBean(@ScriptParameter(description = "bean名称") String name, @ScriptParameter(description = "Class类型") Class<T> clazz) {
        return context.getBean(name, clazz);
    }

    @ScriptFunction(
            name = "getBeans",
            description = "获取spring bean对象列表")
    public <T> List<T> getBeans(@ScriptParameter(description = "Class类型") Class<T> clazz) {
        return context.getBeans(clazz);
    }

    @ScriptFunction(
            name = "getRecordById",
            description = "根据流程id获取流程记录")
    public FlowRecord getRecordById(@ScriptParameter(description = "流程记录id") long id) {
        return context.getRecordById(id);
    }

    @ScriptFunction(
            name = "getOperatorById",
            description = "根据用户id获取流程用户对象")
    public IFlowOperator getOperatorById(@ScriptParameter(description = "用户id") long userId) {
        return context.getOperatorById(userId);
    }

    @ScriptFunction(
            name = "findOperatorsByIds",
            description = "根据用户id获取流程用户列表"
    )
    public List<IFlowOperator> findOperatorsByIds(@ScriptParameter(description = "用户id列表") List<Long> ids) {
        return context.findOperatorsByIds(ids);
    }
}
