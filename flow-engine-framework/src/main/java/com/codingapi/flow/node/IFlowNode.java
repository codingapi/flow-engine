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
     * 是否执行节点
     * @param session 会话
     * @return true: 继续执行下一个节点
     */
    boolean isContinueTrigger(FlowSession session);

    /**
     * 节点验证会话
     */
    void verifySession(FlowSession session);

    /**
     * 构建当前节点下的流程记录，不需要创建记录的返回 空集合
     * @param session 会话
     * @return 流程记录
     */
    List<FlowRecord> generateCurrentRecords(FlowSession session);

    /**
     * 获取节点操作
     * @return 节点操作
     */
    ActionManager actions();

    /**
     * 节点是否完成
     * @param session 会话
     * @return true: 节点完成
     */
    boolean isDone(FlowSession session);

    /**
     * 填充流程记录
     * @param session 会话
     * @param flowRecord 流程记录
     */
    void fillNewRecord(FlowSession session,FlowRecord flowRecord);
}
