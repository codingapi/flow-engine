package com.codingapi.flow.record;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 *  流程流转记录数据模型
 */
@Getter
@Setter
@AllArgsConstructor
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
     * 动作类型
     */
    private ActionType actionType;

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
     * 当前节点下的排序,用于多人审批时控制节点的执行顺序
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
     * 并行id
     */
    private String parallelId;

    /**
     * 并行分支节点id
     */
    private String parallelBranchNodeId;

    /**
     * 并行分支数量
     */
    private int parallelBranchTotal;


    public FlowRecord(FlowSession flowSession, int nodeOrder) {
        IFlowAction action = flowSession.getCurrentAction();
        this.workCode = flowSession.getWorkCode();
        this.workBackupId = flowSession.getBackupId();
        this.nodeId = flowSession.getCurrentNodeId();
        this.nodeType = flowSession.getCurrentNodeType();
        this.formData = flowSession.getFormData().toMapData();
        this.nodeOrder = nodeOrder;
        this.processId = RandomUtils.generateStringId();
        this.createOperatorId = flowSession.getCreatedOperator().getUserId();
        this.recordState = SATE_RECORD_TODO;
        this.actionId = action.id();
        this.actionType = action.type();
        this.currentOperatorId = flowSession.getCurrentOperator().getUserId();
        this.interferedOperatorId = flowSession.getCurrentOperator().entrustOperator() != null ? flowSession.getCurrentOperator().entrustOperator().getUserId() : 0;
        this.advice = flowSession.getAdvice().getAdvice();
        this.signKey = flowSession.getAdvice().getSignKey();
        this.flowState = SATE_FLOW_RUNNING;
        this.createTime = System.currentTimeMillis();
        this.isInterfere = flowSession.getWorkflow().isInterfere();
        this.hidden = false;

        flowSession.getCurrentNode().fillNewRecord(flowSession, this);
        this.extendsRecord(flowSession.getCurrentRecord());

        this.verify();
    }


    /**
     * 继承记录
     * @param record 传递的记录
     */
    public void extendsRecord(FlowRecord record) {
        if (record != null) {
            this.parallelBranchNodeId = record.parallelBranchNodeId;
            this.parallelBranchTotal = record.parallelBranchTotal;
            this.parallelId = record.parallelId;
            this.fromId = record.id;
            this.processId = record.processId;
        }
    }

    /**
     * 当满足条件以后需要清空并行的记录数据
     */
    public void clearParallel() {
        this.parallelBranchNodeId = null;
        this.parallelBranchTotal = 0;
        this.parallelId = null;
    }


    /**
     * 并行分支节点
     *
     * @param parallelBranchNodeId 并行分支节点id
     * @param parallelBranchCount  并行分支数量
     */
    public void parallelBranchNode(String parallelBranchNodeId, int parallelBranchCount, String parallelId) {
        this.parallelBranchNodeId = parallelBranchNodeId;
        this.parallelBranchTotal = parallelBranchCount;
        this.parallelId = parallelId;
    }

    public void verify() {
        if (!StringUtils.hasText(workCode)) {
            throw FlowValidationException.required("workCode");
        }
        if (!StringUtils.hasText(nodeId)) {
            throw FlowValidationException.required("nodeId");
        }
        if (!StringUtils.hasText(title)) {
            throw FlowValidationException.required("title");
        }
        if (!StringUtils.hasText(processId)) {
            throw FlowValidationException.required("processId");
        }
        if (createTime <= 0) {
            throw FlowValidationException.required("createTime");
        }
        if (fromId < 0) {
            throw FlowValidationException.required("fromId");
        }
        if (formData == null) {
            throw FlowValidationException.required("formData");
        }
        if (createOperatorId <= 0) {
            throw FlowValidationException.required("createOperator");
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
     * @param flowSession 流程会话
     * @param done     是否完成
     */
    public void update(FlowSession flowSession, boolean done) {
        IFlowAction flowAction = flowSession.getCurrentAction();
        FlowAdvice flowAdvice = flowSession.getAdvice();
        this.formData = flowSession.getFormData().toMapData();
        this.actionId = flowAction.id();
        this.actionType = flowAction.type();
        this.readable = true;
        this.readTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.recordState = done ? SATE_RECORD_DONE : SATE_RECORD_TODO;

        if(flowAdvice!=null) {
            this.advice = flowAdvice.getAdvice();
            this.signKey = flowAdvice.getSignKey();
        }
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

    /**
     * 转换为FlowAdvice
     *
     * @param workflow 流程设计器
     * @return FlowAdvice
     */
    public FlowAdvice toAdvice(Workflow workflow) {
        FlowAdvice flowAdvice = new FlowAdvice(advice);
        flowAdvice.setSignKey(signKey);
        IFlowNode flowNode = workflow.getFlowNode(nodeId);
        IFlowAction flowAction = flowNode.actionManager().getActionById(actionId);
        flowAdvice.setAction(flowAction);
        return flowAdvice;
    }

    public void resetAddAudit(long fromId, int nodeOrder, long currentOperatorId, boolean hidden) {
        this.fromId = fromId;
        this.nodeOrder = nodeOrder;
        this.currentOperatorId = currentOperatorId;
        this.hidden = hidden;
    }

    public FlowRecord copy(FlowSession flowSession) {
        FlowRecord flowRecord = new FlowRecord(flowSession, 0);
        flowRecord.currentOperatorId = flowSession.getCurrentOperator().getUserId();
        return flowRecord;
    }
}
