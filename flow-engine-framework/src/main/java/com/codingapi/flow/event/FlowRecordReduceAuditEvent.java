package com.codingapi.flow.event;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 减签事件
 * <p>
 * 审批过程中由当前审批人执行减签时触发，登记被撤下的审批人记录与执行人。
 * 被撤下记录的待办与合并关系由保存流程记录的同一批操作清理，订阅方据此同步外部待办。
 */
@Getter
@AllArgsConstructor
public class FlowRecordReduceAuditEvent implements IFlowEvent {

    /**
     * 被撤下的审批记录（已标记撤销）
     */
    private final FlowRecord flowRecord;

    /**
     * 执行减签的审批人id
     */
    private final long reduceOperatorId;

    /**
     * 是否为模拟环境
     */
    private final boolean mock;
}
