package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
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
     * 获取下一个节点列表
     * @param session 会话
     * @return 下一个节点列表
     */
    List<IFlowNode> nextNodes(FlowSession session);

    /**
     * 执行节点
     */
    void execute(FlowSession session, FlowRecordRepository flowRecordRepository);

    /**
     * 节点验证会话
     */
    void verifySession(FlowSession session);

    /**
     * 节点是否继续
     * @param session 会话
     * @return 节点是否继续
     */
    boolean continueNode(FlowSession session);


    /**
     * 生成下一个节点记录
     * @param session 会话
     * @return 下一个节点记录
     */
    List<FlowRecord> generateNextRecords(FlowSession session);
}
