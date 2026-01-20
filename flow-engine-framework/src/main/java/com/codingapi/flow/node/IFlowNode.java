package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.manager.ActionManager;
import com.codingapi.flow.record.FlowRecord;
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
    void verifyNode(FormMeta form);

    /**
     * 执行节点
     * @param session 会话
     * @return true: 继续执行下一个节点
     */
    boolean trigger(FlowSession session);

    /**
     * 节点验证会话
     */
    void verifySession(FlowSession session);


    /**
     * 构建当前节点下的流程记录
     * @param session 会话
     * @return 流程记录
     */
    List<FlowRecord> generateNextRecords(FlowSession session);


    /**
     * 获取节点操作
     * @return 节点操作
     */
    ActionManager actions();

}
