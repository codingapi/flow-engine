package com.codingapi.flow.script.runtime;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class FlowScriptContext {

    @Getter
    private static final FlowScriptContext instance = new FlowScriptContext();

    private FlowScriptContext() {
        this.beanFactory = new IBeanFactory() {
            @Override
            public <T> T getBean(Class<T> clazz) {
                return null;
            }

            @Override
            public <T> T getBean(String name, Class<T> clazz) {
                return null;
            }

            @Override
            public <T> List<T> getBeans(Class<T> clazz) {
                return List.of();
            }
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

    public IFlowOperator getOperatorById(long userId) {
        return beanFactory.getOperatorById(userId);
    }

    public List<IFlowOperator> findByIds(long ... ids){
        return beanFactory.findByIds(ids);
    }

    public List<IFlowOperator> findByIds(List<Long> ids){
        return beanFactory.findByIds(ids);
    }

}
