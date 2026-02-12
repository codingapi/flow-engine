package com.codingapi.flow.infra.pojo;

import com.codingapi.flow.infra.entity.FlowRecordEntity;
import com.codingapi.flow.infra.entity.FlowTodoRecordEntity;
import com.codingapi.flow.record.FlowRecord;
import lombok.Data;

@Data
public class FlowRecordContent {

    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 工作id
     */
    private Long workBackupId;
    /**
     * 流程编码
     */
    private String workCode;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private Long readTime;

    /**
     * 当前审批人Id
     */
    private Long currentOperatorId;

    /**
     * 当前审批人名称
     */
    private String currentOperatorName;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 发起者id
     */
    private Long createOperatorId;
    /**
     * 发起者名称
     */
    private String createOperatorName;

    /**
     * 合并记录id,当存在合并记录数据时待办记录数量不会增加，内容会更新至最新的待办数据信息。
     * {@link FlowRecord#getMergeKey()}
     */
    private String mergeKey;

    /**
     * 合并记录数量
     */
    private Integer margeCount;

    /**
     * 是否可合并
     */
    private Boolean mergeable;

    /**
     * 待办记录id
     */
    private Long recordId;

    /**
     * 超时到期时间
     */
    private Long timeoutTime;

    /**
     * 节点状态 | 待办、已办
     */
    private Integer recordState;
    /**
     * 流程状态 | 运行中、已完成、异常、删除
     */
    private Integer flowState;

    /**
     * 是否抄送
     */
    private Boolean notify;


    public static FlowRecordContent convert(FlowTodoRecordEntity todoRecord){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(todoRecord.getProcessId());
        content.setWorkBackupId(todoRecord.getWorkBackupId());
        content.setWorkCode(todoRecord.getWorkCode());
        content.setNodeId(todoRecord.getNodeId());
        content.setNodeType(todoRecord.getNodeType());
        content.setTitle(todoRecord.getTitle());
        content.setReadTime(todoRecord.getReadTime());
        content.setCurrentOperatorId(todoRecord.getCurrentOperatorId());
        content.setCurrentOperatorName(todoRecord.getCurrentOperatorName());
        content.setCreateTime(todoRecord.getCreateTime());
        content.setCreateOperatorId(todoRecord.getCreateOperatorId());
        content.setCreateOperatorName(todoRecord.getCreateOperatorName());
        content.setMergeKey(todoRecord.getMergeKey());
        content.setMargeCount(todoRecord.getMargeCount());
        content.setMergeable(todoRecord.getMergeable());
        content.setRecordId(todoRecord.getRecordId());
        content.setTimeoutTime(todoRecord.getTimeoutTime());
        content.setRecordState(FlowRecord.SATE_RECORD_TODO);
        content.setFlowState(FlowRecord.SATE_FLOW_RUNNING);
        content.setNotify(false);
        return content;
    }


    public static FlowRecordContent convert(FlowRecordEntity record){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(record.getProcessId());
        content.setWorkBackupId(record.getWorkBackupId());
        content.setWorkCode(record.getWorkCode());
        content.setNodeId(record.getNodeId());
        content.setNodeType(record.getNodeType());
        content.setTitle(record.getTitle());
        content.setReadTime(record.getReadTime());
        content.setCurrentOperatorId(record.getCurrentOperatorId());
        content.setCurrentOperatorName(record.getCurrentOperatorName());
        content.setCreateTime(record.getCreateTime());
        content.setCreateOperatorId(record.getCreateOperatorId());
        content.setCreateOperatorName(record.getCreateOperatorName());
        content.setMergeable(record.getMergeable());
        content.setRecordId(record.getId());
        content.setTimeoutTime(record.getTimeoutTime());
        content.setRecordState(record.getRecordState());
        content.setFlowState(record.getFlowState());
        content.setNotify(record.getNotify());
        return content;
    }
}
