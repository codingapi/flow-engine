package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.IAsyncEvent;

/**
 * 流程触发的异步事件
 */
public interface IFlowEvent extends IAsyncEvent {

    boolean isMock();

    /**
     * 事件分区键，用于保证同一流程内的事件按产生顺序投递。
     *
     * <p>默认为流程id（{@code processId}）：同一流程的事件严格串行，不同流程可并行。
     * 无法确定流程id的事件返回 null，归入同一默认分区。</p>
     *
     * @return 分区键
     */
    default String eventKey() {
        return null;
    }
}
