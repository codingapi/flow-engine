package com.codingapi.flow.manager;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点策略管理
 */
public class NodeStrategyManager {

    @Getter
    private final List<INodeStrategy> strategies;

    public NodeStrategyManager(List<INodeStrategy> strategies) {
        this.strategies = strategies;
    }

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
                return ((RecordMergeStrategy) strategy).isEnable();
            }
        }
        return false;
    }

    /**
     * 是否按顺序执行的审批策略
     */
    public boolean isSequenceMultiOperatorType() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof MultiOperatorAuditStrategy multiOperatorAuditStrategy) {
                return multiOperatorAuditStrategy.getType() == MultiOperatorAuditStrategy.Type.SEQUENCE;
            }
        }
        return false;
    }


    /**
     * 审批意见是否必填
     */
    public boolean isAdviceRequired() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof AdviceStrategy) {
                return ((AdviceStrategy) strategy).isAdviceRequired();
            }
        }
        return false;
    }

    /**
     * 签名是否必填
     */
    public boolean isSignRequired() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof AdviceStrategy) {
                return ((AdviceStrategy) strategy).isSignRequired();
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


    /**
     * 多操作者审批类型
     */
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
     * 多操作者审批并签比例
     */
    public float getMultiOperatorAuditMergePercent() {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof MultiOperatorAuditStrategy) {
                return ((MultiOperatorAuditStrategy) strategy).getPercent();
            }
        }
        return 0;
    }

    public void verifyNode(FlowForm form) {
        for (INodeStrategy strategy : strategies) {
            strategy.verifyNode(form);
        }
    }

    public String generateTitle(FlowSession session) {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof NodeTitleStrategy) {
                return ((NodeTitleStrategy) strategy).generateTitle(session);
            }
        }
        return null;
    }

    public OperatorManager loadOperators(FlowSession session) {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof OperatorLoadStrategy) {
                return ((OperatorLoadStrategy) strategy).loadOperators(session);
            }
        }
        return new OperatorManager(new ArrayList<>());
    }

    public void verifySession(FlowSession session) {
        for (INodeStrategy strategy : strategies) {
            strategy.verifySession(session);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends INodeStrategy> T getStrategy(Class<T> clazz) {
        for (INodeStrategy strategy : strategies) {
            if (strategy.getClass() == clazz) {
                return (T) strategy;
            }
        }
        return null;
    }

    /**
     * 错误触发(没有匹配到人时执行的逻辑)
     *
     * @param session 触发会话
     * @return 错误触发
     */
    public ErrorThrow errorTrigger(FlowSession session) {
        List<INodeStrategy> strategies = this.strategies;
        for (INodeStrategy strategy : strategies) {
            if (strategy instanceof ErrorTriggerStrategy) {
                return ((ErrorTriggerStrategy) strategy).errorTrigger(session);
            }
        }
        return null;
    }
}
