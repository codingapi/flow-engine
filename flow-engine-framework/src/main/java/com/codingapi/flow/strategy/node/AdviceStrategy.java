package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
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
     * 意见必填
     */
    private boolean adviceRequired;
    /**
     * 签名必填
     */
    private boolean signRequired;


    public static AdviceStrategy defaultStrategy() {
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceRequired(false);
        strategy.setSignRequired(false);
        return strategy;
    }

    @Override
    public void copy(INodeStrategy target) {
        this.adviceRequired = ((AdviceStrategy) target).adviceRequired;
        this.signRequired = ((AdviceStrategy) target).signRequired;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("adviceRequired", adviceRequired);
        map.put("signRequired", signRequired);
        return map;
    }

    public static AdviceStrategy fromMap(Map<String, Object> map) {
        AdviceStrategy strategy = IMapConvertor.fromMap(map, AdviceStrategy.class);
        if (strategy == null) return null;
        strategy.setAdviceRequired((boolean) map.get("adviceRequired"));
        strategy.setSignRequired((boolean) map.get("signRequired"));
        return strategy;
    }
}
