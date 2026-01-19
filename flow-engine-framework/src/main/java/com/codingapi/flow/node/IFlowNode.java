package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.session.FlowSession;

import java.util.List;
import java.util.Map;

/**
 * 流程节点
 */
public interface IFlowNode {

    /**
     * 节点id
     */
    String getId();

    /**
     * 节点名称
     */
    String getName();

    /**
     * 流程类型
     */
    String getType();

    /**
     * 转化为map
     */
    Map<String, Object> toMap();

    /**
     * 节点验证
     */
    void verify(FormMeta form);


    /**
     * 获取下一个节点列表
     * @param session 会话
     * @return 下一个节点列表
     */
    List<IFlowNode> nextNodes(FlowSession session);

}
