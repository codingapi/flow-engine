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

    /**
     *  延迟任务id
     */
    private final String id;
    /**
     *  创建时间
     */
    private final long createTime;
    /**
     *  触发时间
     */
    private final long triggerTime;
    /**
     *  当前记录id
     */
    private final long currentRecordId;
    /**
     *  流程编号
     */
    private final String workCode;
    /**
     *  延迟节点id
     */
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
