package com.codingapi.flow.node;

import com.codingapi.flow.session.FlowSession;

/**
 *  配置节点
 *  包括：子流程节点、延迟节点、触发节点
 */
public interface IConfigNode extends IFlowNode {

    /**
     *  执行配置任务，子流程节点、触发节点 执行任务以后要继续执行下一环节
     *  @param flowSession 当前会话
     */
    void execute(FlowSession flowSession);

}
