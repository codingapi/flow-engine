package com.codingapi.flow.record;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.user.IFlowOperator;
import lombok.Getter;

/**
 * 审批意见
 */
@Getter
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
}
