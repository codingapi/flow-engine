package com.codingapi.flow.action;


import com.codingapi.flow.common.ICopyAbility;
import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;

import java.util.List;

/**
 * 节点动作
 */
public interface IFlowAction extends IMapConvertor, ICopyAbility<IFlowAction> {

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
     * 是否可用
     */
    boolean enable();

    /**
     * 执行动作
     */
    List<FlowRecord> generateRecords(FlowSession flowSession);


    /**
     * 执行动作
     * 业务流程的处理入口时通过run函数触发开启的流程
     *
     * @param flowSession 会话
     */
    void run(FlowSession flowSession);

}
