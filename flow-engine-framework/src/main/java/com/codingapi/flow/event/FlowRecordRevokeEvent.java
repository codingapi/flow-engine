package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程撤销事件
 * <p>
 * 撤销流程时触发,登记被撤销的下级记录数据
 */
@Getter
@AllArgsConstructor
public class FlowRecordRevokeEvent implements IFlowEvent {

    /**
     * 被撤销的待办记录
     */
    private final FlowRecord currentRecord;

    /**
     * 是否为模拟环境
     */
    private final boolean mock;
}
