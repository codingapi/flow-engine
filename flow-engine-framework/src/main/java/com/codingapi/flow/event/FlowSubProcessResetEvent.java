package com.codingapi.flow.event;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 子流程重置事件
 * <p>主流程节点上执行子流程数据重置时推送，提醒业务订阅方子流程数据已被重置：
 * 被取代旧组中的选中实例已重建为全新子流程（新流程id），未选中实例以继承方式沿用原结果。</p>
 *
 * <p>订阅方收敛规则：事件经异步线程池分发、到达顺序不保证，应按
 * 「同一父记录 + 节点维度下最大聚合组 id 为活跃组」做幂等覆盖；
 * 事件的 {@link #oldRecord} id 与订阅方当前活跃组 id 不一致时，说明存在丢失的重置事件，
 * 应通过节点记录查询全量对账。</p>
 */
@Getter
@AllArgsConstructor
public class FlowSubProcessResetEvent implements IFlowEvent {

    /**
     * 被重置取代的旧聚合组快照（含全部旧实例的流程id，推送前已做不可变快照）
     */
    private final SubProcessRecord oldRecord;

    /**
     * 重置后的新聚合组快照（继承实例沿用原流程id，重建实例为新流程id，
     * 重建实例的 sourceProcessId 记录其替换的旧实例流程id）
     */
    private final SubProcessRecord newRecord;

    /**
     * 执行重置动作的主流程记录id
     */
    private final long resetRecordId;

    /**
     * 重置操作人
     */
    private final IFlowOperator resetOperator;

    /**
     * 是否模拟环境
     */
    private final boolean mock;
}
