package com.codingapi.flow.record;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批意见
 */
@Data
@NoArgsConstructor
public class FlowAdvice {
    /**
     * 流程动作
     */
    private IFlowAction action;
    /**
     * 审批意见
     */
    private String advice;
    /**
     * 创建者
     */
    private IFlowOperator operator;
    /**
     * 记录时间
     */
    private long createTime;


    public FlowAdvice(IFlowAction action, String advice, IFlowOperator operator) {
        this.action = action;
        this.advice = advice;
        this.operator = operator;
        this.createTime = System.currentTimeMillis();
    }
}
