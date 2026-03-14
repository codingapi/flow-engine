package com.codingapi.flow.script.runtime;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  脚本运行时的$bind上下文对象
 */
public class FlowScriptContext {

    @Getter
    private static final FlowScriptContext instance = new FlowScriptContext();

    private FlowScriptContext() {
        this.beanFactory = new IBeanFactory() {
        };
    }

    @Setter
    private IBeanFactory beanFactory;


    public <T> T getBean(Class<T> clazz) {
        return beanFactory.getBean(clazz);
    }

    public <T> T getBean(String name, Class<T> clazz) {
        return beanFactory.getBean(name, clazz);
    }

    public <T> List<T> getBeans(Class<T> clazz) {
        return beanFactory.getBeans(clazz);
    }

    public FlowRecord getRecordById(long id) {
        return beanFactory.getRecordById(id);
    }

    public IFlowOperator getOperatorById(long userId) {
        return beanFactory.getOperatorById(userId);
    }

    public List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
        return beanFactory.findOperatorsByIds(ids);
    }

}
