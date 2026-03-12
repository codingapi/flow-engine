package com.codingapi.flow.event;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程催办事件
 */
@Getter
@AllArgsConstructor
public class FlowRecordUrgeEvent implements IFlowEvent {

    private final FlowRecord flowRecord;

    private final IFlowOperator urgeOperator;

    private final boolean mock;

}
