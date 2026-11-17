package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程记录保存事件
 * <p>
 * 每次保存流程记录后按记录逐条推送，仅包含实际落库的记录（结束节点虚拟记录不落库，不推送）
 */
@Getter
@AllArgsConstructor
public class FlowRecordSaveEvent implements IFlowEvent {

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
