package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class AdviceStrategy extends BaseStrategy {

    /**
     * 意见必填
     */
    private boolean adviceRequired;
    /**
     * 签名必填
     */
    private boolean signRequired;
    /**
     * 隐藏审批意见，开启后审批时不展示审批意见输入框
     */
    private boolean adviceHidden;

    public static AdviceStrategy defaultStrategy() {
        AdviceStrategy strategy = new AdviceStrategy();
        strategy.setAdviceRequired(false);
        strategy.setSignRequired(false);
        strategy.setAdviceHidden(false);
        return strategy;
    }

    @Override
    public void copy(INodeStrategy target) {
        this.adviceRequired = ((AdviceStrategy) target).adviceRequired;
        this.signRequired = ((AdviceStrategy) target).signRequired;
        this.adviceHidden = ((AdviceStrategy) target).adviceHidden;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("adviceRequired", adviceRequired);
        map.put("signRequired", signRequired);
        map.put("adviceHidden", adviceHidden);
        return map;
    }

    public static AdviceStrategy fromMap(Map<String, Object> map) {
        AdviceStrategy strategy = IMapConvertor.fromMap(map, AdviceStrategy.class);
        if (strategy == null) return null;
        strategy.setAdviceRequired((boolean) map.get("adviceRequired"));
        strategy.setSignRequired((boolean) map.get("signRequired"));
        // 兼容无 adviceHidden 字段的存量数据，默认不隐藏
        strategy.setAdviceHidden(map.get("adviceHidden") != null && (boolean) map.get("adviceHidden"));
        return strategy;
    }
}
