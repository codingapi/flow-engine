package com.codingapi.flow.manager;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 节点操作者管理
 *
 * <p>参与人由 {@code (userId, identity)} 共同标识（issue #288）：同一个人员的两个身份
 * 是两个不同的参与人，匹配与查找都必须带上身份。身份为空时与历史「只比 userId」等价。
 */
public class OperatorManager {

    @Getter
    private final List<IFlowOperator> operators;

    public OperatorManager(List<IFlowOperator> operators) {
        if(operators!=null && !operators.isEmpty()) {
            this.operators = operators.stream().filter(Objects::nonNull).toList();
        }else {
            this.operators = new ArrayList<>();
        }
    }

    public boolean isEmpty() {
        return operators.isEmpty();
    }

    /**
     * 判断操作者是否在节点配置的操作者列表中（按 userId + identity 匹配）
     */
    public boolean match(IFlowOperator operator) {
        if (operator == null) {
            return false;
        }
        return this.getOperator(operator.getUserId(), OperatorIdentity.of(operator)) != null;
    }

    /**
     * 按人员 id 查找操作者（默认身份）
     */
    public IFlowOperator getOperator(long userId) {
        return this.getOperator(userId, OperatorIdentity.DEFAULT);
    }

    /**
     * 按 {@code (userId, identity)} 查找操作者
     *
     * @param userId   人员 id
     * @param identity 身份标识，{@code null} 与空串等价
     * @return 匹配的操作者，不存在时返回 {@code null}
     */
    public IFlowOperator getOperator(long userId, String identity) {
        for (IFlowOperator operator : operators) {
            if (operator.getUserId() == userId
                    && OperatorIdentity.same(OperatorIdentity.of(operator), identity)) {
                return operator;
            }
        }
        return null;
    }
}
