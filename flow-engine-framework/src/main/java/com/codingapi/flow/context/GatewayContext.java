package com.codingapi.flow.context;

import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class GatewayContext {

    @Getter
    private final static GatewayContext instance = new GatewayContext();

    private GatewayContext() {
    }

    @Setter
    @Getter
    private FlowOperatorGateway flowOperatorGateway;

    public IFlowOperator getFlowOperator(long userId) {
        return FlowOperatorLocalThreadCache.getInstance().get(userId,()->flowOperatorGateway.get(userId));
    }

    public List<IFlowOperator> findByIds(List<Long> ids) {
        return FlowOperatorLocalThreadCache.getInstance().find(ids,(idList)-> flowOperatorGateway.findByIds(idList));
    }

}
