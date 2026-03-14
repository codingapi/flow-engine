package com.codingapi.flow.script.request;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 *  流程groovy脚本绑定对象 $bind
 *  def run(request){
 *      $bind.getRecordById(1);
 *  }
 */
@AllArgsConstructor
public class GroovyScriptBind {

    private final FlowScriptContext context;

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
