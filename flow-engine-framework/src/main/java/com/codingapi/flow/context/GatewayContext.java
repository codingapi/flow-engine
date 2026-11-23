package com.codingapi.flow.context;

import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
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

    /**
     * 按人员 id 获取操作者（默认身份）。
     *
     * @param userId 人员 id
     * @return 操作者
     */
    public IFlowOperator getFlowOperator(long userId) {
        return this.getFlowOperator(userId, OperatorIdentity.DEFAULT);
    }

    /**
     * 按人员 id 与身份获取操作者（issue #288）。
     *
     * <p>身份为空（{@code null}/空串）时与 {@link #getFlowOperator(long)} 完全等价，
     * 保证存量调用链行为不变。
     *
     * @param userId   人员 id
     * @param identity 身份标识
     * @return 操作者
     */
    public IFlowOperator getFlowOperator(long userId, String identity) {
        return FlowOperatorLocalThreadCache.getInstance()
                .get(userId, identity, () -> flowOperatorGateway.get(userId, identity));
    }

    public List<IFlowOperator> findByIds(List<Long> ids) {
        return FlowOperatorLocalThreadCache.getInstance().find(ids,(idList)-> flowOperatorGateway.findByIds(idList));
    }

}
