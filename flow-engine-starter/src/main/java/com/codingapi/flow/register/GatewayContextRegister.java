package com.codingapi.flow.register;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;

@AllArgsConstructor
public class GatewayContextRegister implements InitializingBean {

    private final FlowOperatorGateway flowOperatorGateway;

    @Override
    public void afterPropertiesSet() throws Exception {
        GatewayContext.getInstance().setFlowOperatorGateway(flowOperatorGateway);
    }
}
