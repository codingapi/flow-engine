package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程启动事件
 */
@Getter
@AllArgsConstructor
public class FlowRecordStartEvent implements IFlowEvent{

    private final FlowRecord flowRecord;

}
