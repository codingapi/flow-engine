package com.codingapi.flow.event;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程二次发起事件
 * <p>
 * 仅当流程管理员对已正常完成的流程记录执行二次发起时触发：源流程记录保持不变，
 * 新流程以源流程的表单数据重新创建并停在开始节点。业务方可据此调整已落库的业务数据状态。
 */
@Getter
@AllArgsConstructor
public class FlowRecordReissueEvent implements IFlowEvent {

    /**
     * 被二次发起的源流程记录（已完成）
     */
    private final FlowRecord sourceRecord;

    /**
     * 二次发起产生的新流程记录（开始节点待办）
     */
    private final FlowRecord newRecord;

    /**
     * 执行二次发起的操作人（流程管理员）
     */
    private final IFlowOperator operator;

    private final boolean mock;

    /**
     * 事件分区键：按二次发起后新流程的流程id串行投递
     */
    @Override
    public String eventKey() {
        return newRecord == null ? null : newRecord.getProcessId();
    }
}
