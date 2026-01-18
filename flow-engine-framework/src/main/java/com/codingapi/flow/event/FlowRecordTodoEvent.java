package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程待办事件
 */
@Getter
@AllArgsConstructor
public class FlowRecordTodoEvent implements IFlowEvent {

    private final FlowRecord flowRecord;

}
