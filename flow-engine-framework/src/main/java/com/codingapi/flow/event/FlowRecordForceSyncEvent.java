package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运行中流程实例强制同步事件
 *
 * <p>流程实例的运行时快照引用由 {@code oldWorkRuntimeId} 换绑为 {@code newWorkRuntimeId} 后触发，
 * 业务方可据此刷新本地缓存或提示流程参与人。每个被同步的实例产生一条事件。</p>
 */
@Getter
@AllArgsConstructor
public class FlowRecordForceSyncEvent implements IFlowEvent {

    /**
     * 被同步的流程记录（实例内任一记录，用于定位流程实例与节点）
     */
    private final FlowRecord flowRecord;

    /**
     * 同步前的运行时快照id
     */
    private final long oldWorkRuntimeId;

    /**
     * 同步后的运行时快照id
     */
    private final long newWorkRuntimeId;

    private final boolean mock;

    /**
     * 事件分区键：按被同步实例的流程id串行投递
     */
    @Override
    public String eventKey() {
        return flowRecord == null ? null : flowRecord.getProcessId();
    }
}
