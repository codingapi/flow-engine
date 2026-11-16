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

    private final boolean mock;

    /**
     * 事件分区键：同一流程内的事件按顺序投递
     */
    @Override
    public String eventKey() {
        return flowRecord.getProcessId();
    }
}
