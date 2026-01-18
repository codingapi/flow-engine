package com.codingapi.flow.pojo.body;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class FlowAdviceBody {

    /**
     * 流程动作
     */
    private String actionId;
    /**
     * 审批意见
     */
    private String advice;

    /**
     * 签名key
     */
    private String signKey;
    /**
     * 操作者
     */
    private long operatorId;

    /**
     * 退回节点
     */
    private String backNodeId;

    /**
     * 转办人员
     */
    private List<Long> transferOperatorIds;

    public FlowAdviceBody(String actionId, String advice, String signKey, long operatorId) {
        this.actionId = actionId;
        this.advice = advice;
        this.signKey = signKey;
        this.operatorId = operatorId;
    }

    public FlowAdviceBody(String actionId, String advice, long operatorId) {
        this.actionId = actionId;
        this.advice = advice;
        this.operatorId = operatorId;
    }

    public void verify() {
        if (actionId == null || actionId.isEmpty()) {
            throw new IllegalArgumentException("action can not be null");
        }
        if (operatorId <= 0) {
            throw new IllegalArgumentException("operatorId can not be null");
        }
    }
}
