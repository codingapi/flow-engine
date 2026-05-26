package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.springboot.framework.script.annotation.ScriptField;
import com.codingapi.springboot.framework.script.annotation.ScriptFunction;
import com.codingapi.springboot.framework.script.annotation.ScriptType;
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
            description = "获取spring bean对象",
            parameters = {
                    @ScriptField(name = "clazz", description = "Class类型")
            })
    public <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    @ScriptFunction(
            name = "getBean",
            description = "获取spring bean对象",
            parameters = {
                    @ScriptField(name = "name", description = "bean名称"),
                    @ScriptField(name = "clazz", description = "Class类型")
            })
    public <T> T getBean(String name, Class<T> clazz) {
        return context.getBean(name, clazz);
    }

    @ScriptFunction(
            name = "getBeans",
            description = "获取spring bean对象列表",
            parameters = {
                    @ScriptField(name = "clazz", description = "Class类型")
            })
    public <T> List<T> getBeans(Class<T> clazz) {
        return context.getBeans(clazz);
    }

    @ScriptFunction(
            name = "getRecordById",
            description = "根据流程id获取流程记录",
            parameters = {
                    @ScriptField(name = "id", description = "流程记录id")
            })
    public FlowRecord getRecordById(long id) {
        return context.getRecordById(id);
    }

    @ScriptFunction(
            name = "getOperatorById",
            description = "根据用户id获取流程用户对象",
            parameters = {
                    @ScriptField(name = "userId", description = "用户id")
            })
    public IFlowOperator getOperatorById(long userId) {
        return context.getOperatorById(userId);
    }

    @ScriptFunction(
            name = "findOperatorsByIds",
            description = "根据用户id获取流程用户列表",
            parameters = {
                    @ScriptField(name = "ids", description = "用户id列表")
            })
    public List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
        return context.findOperatorsByIds(ids);
    }
}
