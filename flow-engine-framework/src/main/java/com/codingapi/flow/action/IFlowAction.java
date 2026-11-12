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
     * 是否展示动作
     */
    boolean show(FlowSession flowSession);

    /**
     * 是否支持合并批量提交。
     *
     * <p>合并审批时同一审批人会同时持有多条流程实例的待办，前端按勾选的记录逐条提交。
     * 目标按流程实例隔离的动作（如减签，候选人来自各流程自己的当前节点待办人）不能批量，
     * 否则会把 A 流程的目标错配到 B 流程。缺省视为支持。
     *
     * @return true 表示支持合并批量提交
     */
    default boolean batchable() {
        return true;
    }


    /**
     * 执行动作
     * 业务流程的处理入口时通过run函数触发开启的流程
     *
     * @param flowSession 会话
     */
    void run(FlowSession flowSession);

}
