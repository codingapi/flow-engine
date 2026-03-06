package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.ICopyAbility;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.session.FlowSession;

/**
 * 节点配置策略
 */
public interface INodeStrategy extends IMapConvertor, ICopyAbility<INodeStrategy> {

    String TYPE_KEY = "strategyType";

    /**
     * 节点验证
     * 用于流程配置完成以后的验证时触发
     */
    void verifyNode(FlowForm form);

    /**
     * 节点验证会话
     * 流程执行continueTrigger之前需要先对判断请求会话的参数是否满足节点参数要求
     */
    void verifySession(FlowSession session);

    default String strategyType() {
        return this.getClass().getSimpleName();
    }

}
