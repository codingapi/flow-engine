package com.codingapi.flow.action;


import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.Map;

/**
 * 节点动作
 */
public interface IFlowAction {

    /**
     * 流程类型
     */
    ActionType type();

    /**
     * 动作id
     */
    String id();

    /**
     * 动作名称
     */
    String title();

    /**
     * 显示名称
     */
    ActionDisplay display();

    /**
     * 执行动作
     */
    List<FlowRecord> trigger(FlowSession flowSession);

    /**
     * 转换为map
     */
    Map<String, Object> toMap();

    /**
     * 流程是否结束
     *
     * @param session        session
     * @param currentRecord  当前审批记录
     * @param currentRecords 当前节点所有人提交的记录
     * @return 是否结束
     */
    boolean isDone(FlowSession session, FlowRecord currentRecord, List<FlowRecord> currentRecords);
}
