package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程结束事件
 */
@Getter
@AllArgsConstructor
public class FlowRecordDoneEvent implements IFlowEvent{

    private final FlowRecord flowRecord;

}
