package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.DelayStrategy;
import com.codingapi.flow.utils.RandomUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 延迟任务
 */
@Getter
@AllArgsConstructor
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
