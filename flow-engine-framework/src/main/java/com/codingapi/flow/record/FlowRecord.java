package com.codingapi.flow.record;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
public class FlowRecord {

    // 待办、已办
    public static int SATE_RECORD_TODO = 0;
    public static int SATE_RECORD_DONE = 1;

    // 运行中、已完成、异常、删除
    public static int SATE_FLOW_RUNNING = 0;
    public static int SATE_FLOW_DONE = 1;
    public static int SATE_FLOW_ERROR = 2;
    public static int SATE_FLOW_DELETE = 3;

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
     * 父节点id
     */
    private long fromId;
    /**
     * 表单数据
     */
    private Map<String,Object> formData;
    /**
     * 消息标题
     */
    private String title;
    /**
     * 读取时间
     */
    private long readTime;
    /**
     *  流程id
     *  每一次流程启动时生成，直到流程结束
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
     * 当前审批人
     */
    private long currentOperatorId;

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
    private IFlowOperator interferedOperator;

    public FlowRecord(FlowSession flowSession,FlowAdvice flowAdvice,long backupId) {
        this.workCode = flowSession.getWorkCode();
        this.workBackupId = backupId;
        this.nodeId = flowSession.getCurrentNodeId();
        this.formData = flowSession.getFormData().toMapData();
        this.fromId = 0;
        this.title = flowSession.getCurrentNode().generateTitle(flowSession);
        this.processId = RandomUtils.generateStringId();
        this.createOperatorId = flowSession.getCreatedOperator().getUserId();
        this.recordState = SATE_RECORD_TODO;
        this.actionId = flowAdvice.getAction().id();
        this.currentOperatorId = flowAdvice.getOperator().getUserId();
        this.advice = flowAdvice.getAdvice();
        this.flowState = SATE_FLOW_RUNNING;
        this.createTime = System.currentTimeMillis();
        this.timeoutTime = flowSession.getCurrentNode().timeoutTime();
        this.mergeable = flowSession.getCurrentNode().isMergeable();
        this.isInterfere = flowSession.getWorkflow().isInterfere();
    }

    public void verify() {
        if(!StringUtils.hasText(workCode)){
            throw new IllegalArgumentException("workCode is null");
        }
        if(!StringUtils.hasText(nodeId)){
            throw new IllegalArgumentException("nodeId is null");
        }
        if(!StringUtils.hasText(title)){
            throw new IllegalArgumentException("title is null");
        }
        if(!StringUtils.hasText(processId)){
            throw new IllegalArgumentException("processId is null");
        }
        if(createTime <= 0){
            throw new IllegalArgumentException("createTime is null");
        }
        if(fromId < 0){
            throw new IllegalArgumentException("fromId is null");
        }
        if(formData == null){
            throw new IllegalArgumentException("formData is null");
        }
        if(createOperatorId <= 0){
            throw new IllegalArgumentException("createOperator is null");
        }
        if(currentOperatorId <= 0){
            throw new IllegalArgumentException("currentOperatorId is null");
        }
        if(actionId == null){
            throw new IllegalArgumentException("actionId is null");
        }
    }

    public boolean isTodo() {
        return recordState == SATE_RECORD_TODO && flowState == SATE_FLOW_RUNNING;
    }
}
