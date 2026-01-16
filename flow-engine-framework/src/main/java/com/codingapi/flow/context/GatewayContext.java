package com.codingapi.flow.context;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

public class GatewayContext {

    @Getter
    private final static GatewayContext instance = new GatewayContext();

    private GatewayContext() {
    }

    @Setter
    @Getter
    private FlowOperatorGateway flowOperatorGateway;

    public IFlowOperator getFlowOperator(long userId) {
        return flowOperatorGateway.get(userId);
    }

}
