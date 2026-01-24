package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 催办间隔控制
 */
@Getter
@AllArgsConstructor
public class UrgeInterval {

    @Setter
    private long id;
    private final String processId;
    private final long recordId;
    private final long createTime;

    public UrgeInterval(FlowRecord flowRecord) {
        this.processId = flowRecord.getProcessId();
        this.recordId = flowRecord.getId();
        this.createTime = flowRecord.getCreateTime();
    }
}
