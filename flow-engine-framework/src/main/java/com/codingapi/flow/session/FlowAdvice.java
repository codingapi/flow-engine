package com.codingapi.flow.session;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 审批意见
 */
@Data
@NoArgsConstructor
public class FlowAdvice {

    /**
     * 审批动作
     */
    private IFlowAction action;

    /**
     * 审批意见
     */
    private String advice;
    /**
     * 签名key
     */
    private String signKey;

    /**
     * 退回节点
     */
    private IFlowNode backNode;

    /**
     * 转办人员
     */
    private List<IFlowOperator> forwardOperators;


    public FlowAdvice(String advice) {
        this.advice = advice;
    }

    public static FlowAdvice nullFlowAdvice() {
        return new FlowAdvice();
    }
}
