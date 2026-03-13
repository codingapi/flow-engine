package com.codingapi.flow.pojo.response;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoRecord;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FlowRecordContent {

    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 工作id
     */
    private Long workflowRuntimeId;
    /**
     * 流程标题
     */
    private String workTitle;
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
     * 节点名称
     */
    private String nodeName;

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
     * 提交审批人Id
     */
    private Long submitOperatorId;

    /**
     * 提交审批人名称
     */
    private String submitOperatorName;

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
     * {@link FlowRecord#getTodoKey()}
     */
    private String todoKey;

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


    public static FlowRecordContent convert(FlowRecord record){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(record.getProcessId());
        content.setWorkflowRuntimeId(record.getWorkRuntimeId());
        content.setWorkCode(record.getWorkCode());
        content.setNodeId(record.getNodeId());
        content.setNodeType(record.getNodeType());
        content.setNodeName(record.getNodeName());
        content.setTitle(record.getTitle());
        content.setReadTime(record.getReadTime());
        content.setCurrentOperatorId(record.getCurrentOperatorId());
        content.setCurrentOperatorName(record.getCurrentOperatorName());
        content.setSubmitOperatorId(record.getSubmitOperatorId());
        content.setSubmitOperatorName(record.getSubmitOperatorName());
        content.setCreateTime(record.getCreateTime());
        content.setCreateOperatorId(record.getCreateOperatorId());
        content.setCreateOperatorName(record.getCreateOperatorName());
        content.setMergeable(record.isMergeable());
        content.setRecordId(record.getId());
        content.setTimeoutTime(record.getTimeoutTime());
        content.setRecordState(record.getRecordState());
        content.setFlowState(record.getFlowState());
        content.setNotify(record.isNotify());
        return content;
    }


    public static FlowRecordContent convert(FlowTodoRecord todoRecord){
        FlowRecordContent content = new FlowRecordContent();
        content.setProcessId(todoRecord.getProcessId());
        content.setWorkflowRuntimeId(todoRecord.getWorkflowRuntimeId());
        content.setWorkCode(todoRecord.getWorkCode());
        content.setNodeId(todoRecord.getNodeId());
        content.setNodeType(todoRecord.getNodeType());
        content.setNodeName(todoRecord.getNodeName());
        content.setTitle(todoRecord.getTitle());
        content.setReadTime(todoRecord.getReadTime());
        content.setCurrentOperatorId(todoRecord.getCurrentOperatorId());
        content.setCurrentOperatorName(todoRecord.getCurrentOperatorName());
        content.setSubmitOperatorId(todoRecord.getSubmitOperatorId());
        content.setSubmitOperatorName(todoRecord.getSubmitOperatorName());
        content.setCreateTime(todoRecord.getCreateTime());
        content.setCreateOperatorId(todoRecord.getCreateOperatorId());
        content.setCreateOperatorName(todoRecord.getCreateOperatorName());
        content.setTodoKey(todoRecord.getTodoKey());
        content.setMargeCount(todoRecord.getMargeCount());
        content.setMergeable(todoRecord.isMergeable());
        content.setRecordId(todoRecord.getRecordId());
        content.setTimeoutTime(todoRecord.getTimeoutTime());
        content.setRecordState(FlowRecord.SATE_RECORD_TODO);
        content.setFlowState(FlowRecord.SATE_FLOW_RUNNING);
        content.setNotify(false);
        return content;
    }
}
