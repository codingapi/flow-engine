package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程删除事件
 * <p>
 * 仅当删除位于开始节点且尚未流转的流程实例时触发
 */
@Getter
@AllArgsConstructor
public class FlowRecordDeleteEvent implements IFlowEvent {

    private final FlowRecord flowRecord;

    private final boolean mock;
}
