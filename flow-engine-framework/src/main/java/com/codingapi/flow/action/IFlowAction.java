package com.codingapi.flow.action;


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
    String type();

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
    List<FlowRecord> generateRecords(FlowSession flowSession);

    /**
     * 转换为map
     */
    Map<String, Object> toMap();


    /**
     * 执行动作
     * 业务流程的处理入口时通过run函数触发开启的流程
     *
     * @param flowSession 会话
     */
    void run(FlowSession flowSession);


    /**
     * 复制动作
     */
    void copy(IFlowAction action);

}
