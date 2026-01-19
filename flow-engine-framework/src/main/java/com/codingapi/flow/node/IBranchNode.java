package com.codingapi.flow.node;

import com.codingapi.flow.session.FlowSession;

/**
 *  条件节点
 */
public interface IBranchNode extends IFlowNode{

    /**
     * 匹配节点
     * @param flowSession 流程会话
     * @return 是否匹配
     */
    boolean match(FlowSession flowSession);

    /**
     * 顺序
     */
    int order();

}
