package com.codingapi.flow.script.runtime;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;

import java.util.List;

public interface IBeanFactory {

    default <T> T getBean(Class<T> clazz) {
        return null;
    }

    default <T> T getBean(String name, Class<T> clazz) {
        return null;
    }

    default <T> List<T> getBeans(Class<T> clazz) {
        return null;
    }

    default FlowRecord getRecordById(long id) {
        return null;
    }

    default IFlowOperator getOperatorById(long userId) {
        return GatewayContext.getInstance().getFlowOperator(userId);
    }

    /**
     * 按 {@code (userId, identity)} 获取操作者（issue #288）。
     *
     * <p>脚本需要「让某人以指定身份参与」时使用，例如审批人脚本返回
     * {@code [$bind.getOperatorByIdentity(2, 'department1')]}。
     *
     * @param userId   人员 id
     * @param identity 身份标识，{@code null} 与空串等价（默认身份）
     * @return 操作者
     */
    default IFlowOperator getOperatorById(long userId, String identity) {
        return GatewayContext.getInstance().getFlowOperator(userId, identity);
    }


    default List<IFlowOperator> findOperatorsByIds(List<Long> ids) {
        return GatewayContext.getInstance().findByIds(ids);
    }

}
