package com.codingapi.flow.record;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;

/**
 * 流程配置
 */
@Getter
public class FlowConfigure {
    /**
     * 超时到期时间
     */
    private long timeoutTime;
    /**
     * 是否可合并
     */
    private boolean mergeable;
    /**
     * 是否干预
     */
    private boolean isInterfere;
    /**
     * 被干预的用户
     */
    private IFlowOperator interferedOperator;
}
