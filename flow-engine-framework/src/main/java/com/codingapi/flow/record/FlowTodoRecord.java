package com.codingapi.flow.record;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 *  待办记录数据，所有的待办列表
 */
@Getter
@AllArgsConstructor
public class FlowTodoRecord {

    /**
     * 合并记录id
     */
    @Setter
    private long id;

    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 工作id
     */
    private long workBackupId;
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
    private long readTime;

    /**
     * 当前审批人Id
     */
    private long currentOperatorId;

    /**
     * 当前审批人名称
     */
    private String currentOperatorName;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 发起者id
     */
    private long createOperatorId;
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
    private int margeCount;

    /**
     * 是否可合并
     */
    private boolean mergeable;

    /**
     * 待办记录id
     */
    private long recordId;

    /**
     * 超时到期时间
     */
    private long timeoutTime;


    public FlowTodoRecord(FlowRecord flowRecord) {
       this.update(flowRecord);
    }


    public void update(FlowRecord flowRecord){
        this.processId = flowRecord.getProcessId();
        this.workBackupId = flowRecord.getWorkBackupId();
        this.workCode = flowRecord.getWorkCode();
        this.nodeId = flowRecord.getNodeId();
        this.nodeType = flowRecord.getNodeType();
        this.title = flowRecord.getTitle();
        this.readTime = flowRecord.getReadTime();
        this.currentOperatorId = flowRecord.getCurrentOperatorId();
        this.currentOperatorName = flowRecord.getCurrentOperatorName();
        this.createTime = flowRecord.getCreateTime();
        this.createOperatorId = flowRecord.getCreateOperatorId();
        this.createOperatorName = flowRecord.getCreateOperatorName();
        this.mergeKey = flowRecord.getMergeKey();
        this.mergeable = flowRecord.isMergeable();
        this.recordId = flowRecord.getId();
        this.timeoutTime = flowRecord.getTimeoutTime();
    }

    public void addMargeCount(){
        if(this.mergeable) {
            this.margeCount++;
        }
    }

    public void divMargeCount(){
        if(this.mergeable) {
            this.margeCount--;
        }
    }

    public boolean hasMargeCount(){
        return this.mergeable && this.margeCount >= 0;
    }

}
