package com.codingapi.flow.gateway;

import com.codingapi.flow.operator.IFlowOperator;

import java.util.List;

/**
 * 流程操作者防腐层
 */
public interface FlowOperatorGateway {

    /**
     * 按人员 id 获取操作者（默认身份）。
     *
     * @param id 人员 id
     * @return 操作者
     */
    IFlowOperator get(long id);

    /**
     * 按人员 id 与身份获取操作者（issue #288）。
     *
     * <p>同一个人员可能以多个身份参与流程（例如同时隶属两个部门），身份由下游自行定义语义，
     * 框架只透传。该方法提供 {@code default} 实现：下游存量实现无需改动即可编译，
     * 行为退化为忽略身份、等同于 {@link #get(long)}。需要支持多身份时再覆盖此方法。
     *
     * @param id       人员 id
     * @param identity 身份标识，{@code null} 与空串等价（默认身份）
     * @return 操作者
     */
    default IFlowOperator get(long id, String identity) {
        return get(id);
    }

    /**
     * 按人员 id 批量获取操作者（默认身份）。
     *
     * @param ids 人员 id 列表
     * @return 操作者列表
     */
    List<IFlowOperator> findByIds(List<Long> ids);

    /**
     * 按人员 id 与统一身份批量获取操作者（issue #288 衍生 / #293）。
     *
     * <p>适用于「同一批人员以同一个身份参与流程」的场景（如同一部门下的多个审批人），
     * 避免逐条调用 {@link #get(long, String)} 造成 N+1 回查。
     * 默认实现忽略身份、退化为 {@link #findByIds(List)}，下游存量实现无需改动即可编译；
     * 需要支持多身份时再覆盖此方法。
     *
     * @param ids      人员 id 列表
     * @param identity 身份标识，{@code null} 与空串等价（默认身份）
     * @return 操作者列表
     */
    default List<IFlowOperator> findByIds(List<Long> ids, String identity) {
        return findByIds(ids);
    }

}
