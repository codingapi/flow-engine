package com.codingapi.flow.register;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class FlowScriptContextRegister implements InitializingBean {

    private final ApplicationContext spring;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
            @Override
            public <T> T getBean(Class<T> clazz) {
                return spring.getBean(clazz);
            }

            @Override
            public <T> T getBean(String name, Class<T> clazz) {
                return spring.getBean(name, clazz);
            }

            @Override
            public <T> List<T> getBeans(Class<T> clazz) {
                Map<String, T> beans = spring.getBeansOfType(clazz);
                return beans.values().stream().toList();
            }

            @Override
            public FlowRecord getRecordById(long id) {
                return flowRecordRepository.get(id);
            }

            @Override
            public IFlowOperator getOperatorById(long userId) {
                return flowOperatorGateway.get(userId);
            }

            @Override
            public List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
                return flowOperatorGateway.findByIds(ids);
            }
        });
    }
}
