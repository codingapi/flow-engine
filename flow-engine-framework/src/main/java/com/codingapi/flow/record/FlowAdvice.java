package com.codingapi.flow.record;

import com.codingapi.flow.action.FlowAction;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.Data;

/**
 * 审批意见
 */
@Data
public class FlowAdvice {
    /**
     * 流程动作
     */
    private FlowAction action;
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
}
