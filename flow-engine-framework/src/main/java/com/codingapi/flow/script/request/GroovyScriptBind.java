package com.codingapi.flow.script.request;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 *  流程groovy脚本绑定对象 $bind
 *  def run(request){
 *      $bind.createErrorThrow(operator);
 *  }
 */
@AllArgsConstructor
public class GroovyScriptBind {

    private final FlowScriptContext context;

    public ErrorThrow createErrorThrow(IFlowNode node) {
        return context.createErrorThrow(node);
    }

    public ErrorThrow createErrorThrow(List<Long> userIds) {
        return context.createErrorThrow(userIds);
    }

    public ErrorThrow createErrorThrow(long... userIds) {
        return context.createErrorThrow(userIds);
    }

    public <T> T getBean(Class<T> clazz) {
        return context.getBean(clazz);
    }

    public <T> T getBean(String name, Class<T> clazz) {
        return context.getBean(name, clazz);
    }

    public <T> List<T> getBeans(Class<T> clazz) {
        return context.getBeans(clazz);
    }

    public FlowRecord getRecordById(long id) {
        return context.getRecordById(id);
    }

    public IFlowOperator getOperatorById(long userId) {
        return context.getOperatorById(userId);
    }

    public List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
        return context.findOperatorsByIds(ids);
    }
}
