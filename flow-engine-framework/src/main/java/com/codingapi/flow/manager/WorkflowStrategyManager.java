package com.codingapi.flow.manager;

import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;
import com.codingapi.flow.strategy.workflow.InterfereStrategy;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 流程策略管理
 */
@AllArgsConstructor
public class WorkflowStrategyManager {

    private final List<IWorkflowStrategy> strategies;


    public boolean isEnableInterfere() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if (interfereStrategy != null) {
            return interfereStrategy.isEnable();
        }
        return false;
    }

    public boolean isEnableUrge() {
        InterfereStrategy interfereStrategy = getStrategy(InterfereStrategy.class);
        if (interfereStrategy != null) {
            return interfereStrategy.isEnable();
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public <T extends IWorkflowStrategy> T getStrategy(Class<T> clazz) {
        for (IWorkflowStrategy strategy : strategies) {
            if (strategy.getClass() == clazz) {
                return (T) strategy;
            }
        }
        return null;
    }

    /**
     * 验证操作者（默认身份）
     *
     * @param currentOperator  当前操作者
     * @param recordOperatorId 记录操作者id
     */
    public void verifyOperator(IFlowOperator currentOperator, long recordOperatorId) {
        this.verifyOperator(currentOperator, recordOperatorId, OperatorIdentity.DEFAULT);
    }

    /**
     * 验证操作者。
     *
     * <p>参与人由 {@code (userId, identity)} 共同决定（issue #288）：同一个人员的两个身份是
     * 两个不同的参与人，只比 userId 会让 user1 以 department1 的身份操作 department2 的待办。
     * 身份为空时与历史「只比 userId」的行为完全一致。
     *
     * @param currentOperator       当前操作者
     * @param recordOperatorId      记录操作者id
     * @param recordOperatorIdentity 记录操作者身份标识，{@code null} 与空串等价
     */
    public void verifyOperator(IFlowOperator currentOperator, long recordOperatorId, String recordOperatorIdentity) {
        boolean operatorMatched = recordOperatorId == currentOperator.getUserId()
                && OperatorIdentity.same(recordOperatorIdentity, OperatorIdentity.of(currentOperator));
        if (!this.isEnableInterfere()) {
            if (!operatorMatched) {
                throw FlowStateException.operatorNotMatch();
            }
        } else {
            if (!currentOperator.isFlowManager()) {
                if (!operatorMatched) {
                    throw FlowStateException.operatorNotMatch();
                }
            }
        }
    }
}
