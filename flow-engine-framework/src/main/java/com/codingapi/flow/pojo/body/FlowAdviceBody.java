package com.codingapi.flow.pojo.body;

import lombok.Data;

@Data
public class FlowAdviceBody {

    /**
     * 流程动作
     */
    private String action;
    /**
     * 审批意见
     */
    private String advice;
    /**
     * 创建者
     */
    private long operatorId;
}
