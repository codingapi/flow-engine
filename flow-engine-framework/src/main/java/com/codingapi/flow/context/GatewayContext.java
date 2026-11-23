package com.codingapi.flow.context;

import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.operator.OperatorIdentityGuard;
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
        IFlowOperator operator = FlowOperatorLocalThreadCache.getInstance()
                .get(userId, identity, () -> flowOperatorGateway.get(userId, identity));
        // 下游未覆盖带身份的取人方法时身份会被静默丢弃，这里补一条去重告警（issue #288 衍生 / #294）
        OperatorIdentityGuard.check(userId, identity, operator);
        return operator;
    }

    public List<IFlowOperator> findByIds(List<Long> ids) {
        return FlowOperatorLocalThreadCache.getInstance().find(ids,(idList)-> flowOperatorGateway.findByIds(idList));
    }

    /**
     * 按身份批量获取操作者（issue #288 衍生 / #293）。
     *
     * <p>整批人员使用同一个身份，结果按 {@code (id, identity)} 缓存，
     * 避免多身份场景下同一批人员被逐条回查。
     *
     * @param ids      人员 id 列表
     * @param identity 身份标识，{@code null} 与空串等价（默认身份）
     * @return 操作者列表
     */
    public List<IFlowOperator> findByIds(List<Long> ids, String identity) {
        return FlowOperatorLocalThreadCache.getInstance()
                .find(ids, identity, (idList) -> flowOperatorGateway.findByIds(idList, identity));
    }

}
