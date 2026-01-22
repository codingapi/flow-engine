package com.codingapi.flow.delay;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.DelayStrategy;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

@Getter
public class DelayTask {

    private final String id;
    private final long createTime;
    private final long triggerTime;
    private final long currentRecordId;
    private final String workCode;
    private final String delayNodeId;

    public DelayTask(DelayStrategy delayStrategy, FlowRecord flowRecord, String delayNodeId) {
        this.id = RandomUtils.generateStringId();
        this.delayNodeId = delayNodeId;
        this.createTime = System.currentTimeMillis();
        this.triggerTime = createTime + delayStrategy.getTriggerTime();
        this.currentRecordId = flowRecord.getId();
        this.workCode = flowRecord.getWorkCode();
    }

}
