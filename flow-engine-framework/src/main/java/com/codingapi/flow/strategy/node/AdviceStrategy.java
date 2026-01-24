package com.codingapi.flow.strategy.node;

import com.codingapi.flow.convert.IMapConvertor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 节点审批意见策略
 */
@Setter
@Getter
@NoArgsConstructor
public class AdviceStrategy extends BaseStrategy {

    /**
     * 是否可空
     */
    private boolean adviceNullable;
    /**
     * 是否可签名
     */
    private boolean signable;


    public static AdviceStrategy defaultStrategy() {
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceNullable(true);
        strategy.setSignable(false);
        return strategy;
    }

    @Override
    public void copy(INodeStrategy target) {
        this.adviceNullable = ((AdviceStrategy) target).adviceNullable;
        this.signable = ((AdviceStrategy) target).signable;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("adviceNullable", adviceNullable);
        map.put("signable", signable);
        return map;
    }

    public static AdviceStrategy fromMap(Map<String, Object> map) {
        AdviceStrategy strategy = IMapConvertor.fromMap(map, AdviceStrategy.class);
        if (strategy == null) return null;
        strategy.setAdviceNullable((boolean) map.get("adviceNullable"));
        strategy.setSignable((boolean) map.get("signable"));
        return strategy;
    }
}
