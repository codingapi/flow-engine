package com.codingapi.flow.strategy.node;

import com.codingapi.flow.common.IMapConvertor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 记录合并策略配置
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecordMergeStrategy extends BaseStrategy {

    /**
     * 合并审批类型
     */
    public enum MergeType {
        /**
         * 审批人合并：以当前审批人（currentOperatorId）为合并依据
         */
        APPROVER,
        /**
         * 发起人合并：以流程发起人（createOperatorId）为合并依据
         */
        CREATOR,
        /**
         * 提交人合并：以流程提交人（submitOperatorId）为合并依据
         */
        SUBMITTER
    }

    private boolean enable;

    /**
     * 合并类型，默认审批人合并（向后兼容）
     */
    private MergeType mergeType = MergeType.APPROVER;

    public RecordMergeStrategy(boolean enable) {
        this.enable = enable;
        this.mergeType = MergeType.APPROVER;
    }
    // 全参构造器 (boolean, MergeType) 由 @AllArgsConstructor 生成

    @Override
    public void copy(INodeStrategy target) {
        RecordMergeStrategy recordMergeStrategy = (RecordMergeStrategy) target;
        this.enable = recordMergeStrategy.enable;
        this.mergeType = recordMergeStrategy.mergeType;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("enable", enable);
        map.put("mergeType", mergeType.name());
        return map;
    }

    public static RecordMergeStrategy fromMap(Map<String, Object> map) {
        RecordMergeStrategy strategy = IMapConvertor.fromMap(map, RecordMergeStrategy.class);
        if (strategy == null) return null;
        strategy.enable = (boolean) map.get("enable");
        // 兼容旧数据：mergeType 缺失时默认审批人合并
        Object mergeType = map.get("mergeType");
        if (mergeType != null) {
            strategy.setMergeType(MergeType.valueOf((String) mergeType));
        } else {
            strategy.setMergeType(MergeType.APPROVER);
        }
        return strategy;
    }

    public static RecordMergeStrategy defaultStrategy() {
        RecordMergeStrategy strategy = new RecordMergeStrategy();
        strategy.setEnable(false);
        return strategy;
    }
}
