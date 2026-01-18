package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程正常完成事件
 */
@Getter
@AllArgsConstructor
public class FlowRecordFinishEvent implements IFlowEvent{

    private final FlowRecord flowRecord;

}
