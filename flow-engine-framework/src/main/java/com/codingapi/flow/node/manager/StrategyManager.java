package com.codingapi.flow.node.manager;

import com.codingapi.flow.strategy.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class StrategyManager {

    @Getter
    private final List<INodeStrategy> strategies;

    /**
     * 获取超时时间
     */
    public long getTimeoutTime() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof TimeoutStrategy) {
                return System.currentTimeMillis() + ((TimeoutStrategy) strategy).getTimeoutTime();
            }
        }
        return 0;
    }

    /**
     * 是否可合并
     */
    public boolean isMergeable() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof RecordMergeStrategy) {
                return ((RecordMergeStrategy) strategy).isMergeable();
            }
        }
        return false;
    }


    /**
     * 是否恢复到退回节点
     */
    public boolean isResume() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof ResubmitStrategy) {
                return ((ResubmitStrategy) strategy).isResume();
            }
        }
        return false;
    }


    public MultiOperatorAuditStrategy.Type getMultiOperatorAuditStrategyType() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof MultiOperatorAuditStrategy) {
                return ((MultiOperatorAuditStrategy) strategy).getType();
            }
        }
        return null;
    }


    /**
     * 是否是顺序审批
     *
     * @return true/false
     */
    public boolean isSequenceMultiOperator() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof MultiOperatorAuditStrategy) {
                return ((MultiOperatorAuditStrategy) strategy).getType() == MultiOperatorAuditStrategy.Type.SEQUENCE;
            }
        }
        return false;
    }

    public float getMultiOperatorAuditMergePercent() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof MultiOperatorAuditStrategy) {
                return ((MultiOperatorAuditStrategy) strategy).getPercent();
            }
        }
        return 0;
    }
}
