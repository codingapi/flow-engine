package com.codingapi.flow.node.manager;

import com.codingapi.flow.strategy.INodeStrategy;
import com.codingapi.flow.strategy.RecordMergeStrategy;
import com.codingapi.flow.strategy.TimeoutStrategy;
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
}
