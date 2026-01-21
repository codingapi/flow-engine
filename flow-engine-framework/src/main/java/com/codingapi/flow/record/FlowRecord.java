package com.codingapi.flow.record;

import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
@Setter
public class FlowRecord {

    // 待办、已办
    public static int SATE_RECORD_TODO = 0;
    public static int SATE_RECORD_DONE = 1;

    // 运行中、已完成、终止、异常、删除
    public static int SATE_FLOW_RUNNING = 0;
    public static int SATE_FLOW_DONE = 1;
    public static int SATE_FLOW_FINISH = 2;
    public static int SATE_FLOW_ERROR = 3;
    public static int SATE_FLOW_DELETE = 4;

    /**
     * 记录id
     */
    @Setter
    private long id;
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
     * 父节点id
     */
    private long fromId;
    /**
     * 表单数据
     */
    private Map<String, Object> formData;
    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private long readTime;
    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 流程动作
     */
    private String actionId;
    /**
     * 审批意见
     */
    private String advice;

    /**
     * 签名key
     */
    private String signKey;

    /**
     * 当前审批人
     */
    private long currentOperatorId;

    /**
     * 有那个节点退回的
     */
    @Setter
    private String returnNodeId;


    /**
     * 当前节点下的排序
     */
    private int nodeOrder;

    /**
     * 是否隐藏记录（用于多人审批时）
     */
    private boolean hidden;

    /**
     * 节点状态 | 待办、已办
     */
    private int recordState;
    /**
     * 流程状态 | 运行中、已完成、异常、删除
     */
    private int flowState;
    /**
     * 更新时间
     */
    private long updateTime;
    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 完成时间
     */
    private long finishTime;
    /**
     * 是否已读
     */
    private boolean readable;
    /**
     * 发起者id
     */
    private long createOperatorId;
    /**
     * 异常信息
     */
    private String errMessage;

    /**
     * 超时到期时间
     */
    private long timeoutTime;
    /**
     * 是否可合并
     */
    private boolean mergeable;
    /**
     * 是否干预
     */
    private boolean isInterfere;
    /**
     * 被干预的用户
     */
    private long interferedOperatorId;

    /**
     * 并行分支节点id
     */
    private String parallelBranchNodeId;

    /**
     * 并行分支数量
     */
    private int parallelBranchCount;


    public FlowRecord(FlowSession flowSession, String actionId, String processId, long fromId, int nodeOrder) {
        this.workCode = flowSession.getWorkCode();
        this.workBackupId = flowSession.getBackupId();
        this.nodeId = flowSession.getCurrentNodeId();
        this.nodeType = flowSession.getCurrentNodeType();
        this.formData = flowSession.getFormData().toMapData();
        this.fromId = fromId;
        this.nodeOrder = nodeOrder;
        this.processId = processId;
        this.createOperatorId = flowSession.getCreatedOperator().getUserId();
        this.recordState = SATE_RECORD_TODO;
        this.actionId = actionId;
        this.currentOperatorId = flowSession.getCurrentOperator().getUserId();
        this.interferedOperatorId = flowSession.getCurrentOperator().entrustOperator()!=null?flowSession.getCurrentOperator().entrustOperator().getUserId():0;
        this.advice = flowSession.getAdvice().getAdvice();
        this.signKey = flowSession.getAdvice().getSignKey();
        this.flowState = SATE_FLOW_RUNNING;
        this.createTime = System.currentTimeMillis();
        this.isInterfere = flowSession.getWorkflow().isInterfere();
        this.hidden = false;

        flowSession.getCurrentNode().fillNewRecord(flowSession,this);
        this.extendsRecord(flowSession.getCurrentRecord());

        this.verify();
    }


    public void extendsRecord(FlowRecord record) {
        if(record!=null) {
            this.parallelBranchNodeId = record.parallelBranchNodeId;
            this.parallelBranchCount = record.parallelBranchCount;
        }
    }


    /**
     * 并行分支节点
     * @param parallelBranchNodeId 并行分支节点id
     * @param parallelBranchCount 并行分支数量
     */
    public void parallelBranchNode(String parallelBranchNodeId, int parallelBranchCount) {
        this.parallelBranchNodeId = parallelBranchNodeId;
        this.parallelBranchCount = parallelBranchCount;
    }

    public void verify() {
        if (!StringUtils.hasText(workCode)) {
            throw new IllegalArgumentException("workCode is null");
        }
        if (!StringUtils.hasText(nodeId)) {
            throw new IllegalArgumentException("nodeId is null");
        }
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("title is null");
        }
        if (!StringUtils.hasText(processId)) {
            throw new IllegalArgumentException("processId is null");
        }
        if (createTime <= 0) {
            throw new IllegalArgumentException("createTime is null");
        }
        if (fromId < 0) {
            throw new IllegalArgumentException("fromId is null");
        }
        if (formData == null) {
            throw new IllegalArgumentException("formData is null");
        }
        if (createOperatorId <= 0) {
            throw new IllegalArgumentException("createOperator is null");
        }
        if (currentOperatorId <= 0) {
            throw new IllegalArgumentException("currentOperatorId is null");
        }
        if (actionId == null) {
            throw new IllegalArgumentException("actionId is null");
        }
    }

    /**
     * 判断是否待办
     *
     * @return true/false
     */
    public boolean isTodo() {
        return recordState == SATE_RECORD_TODO && flowState == SATE_FLOW_RUNNING && !hidden;
    }


    /**
     * 判断是否已完成
     */
    public boolean isFinish() {
        return flowState != SATE_FLOW_RUNNING;
    }

    /**
     * 判断节点类型
     *
     * @param nodeType 节点类型
     * @return true/false
     */
    public boolean isNodeType(String nodeType) {
        return this.nodeType.equals(nodeType);
    }

    /**
     * 更新记录
     *
     * @param formData 表单数据
     * @param advice   审批意见
     * @param signKey  签名key
     * @param done     是否完成
     */
    public void update(Map<String, Object> formData, String advice, String signKey, boolean done) {
        this.formData = formData;
        this.advice = advice;
        this.signKey = signKey;
        this.readable = true;
        this.readTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.recordState = done ? SATE_RECORD_DONE : SATE_RECORD_TODO;
    }


    /**
     * 流程结束
     */
    public void finish(boolean success) {
        this.flowState = success ? SATE_FLOW_FINISH : SATE_FLOW_DONE;
        this.finishTime = System.currentTimeMillis();
    }


    public void hidden() {
        this.hidden = true;
    }

    public void show() {
        this.hidden = false;
    }

    public boolean isShow() {
        return !hidden;
    }


    /**
     * 判断是否退回
     */
    public boolean isReturnRecord() {
        return StringUtils.hasText(returnNodeId);
    }

    public void resetNodeOrder(int nodeOrder) {
        this.nodeOrder = nodeOrder;
    }
}
