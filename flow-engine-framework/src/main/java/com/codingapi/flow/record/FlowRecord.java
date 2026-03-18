package com.codingapi.flow.record;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FormData;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.session.FlowAdvice;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 流程流转记录数据模型
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
    private long workRuntimeId;

    /**
     * 流程名称
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
     * 来源id
     */
    private long fromId;
    /**
     * 父节点id（用于子流程中）
     */
    private long parentId;

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
     * 流程惟一标识
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
    private String actionType;

    /**
     * 动作名称
     */
    private String actionName;

    /**
     * 审批意见
     */
    private String advice;

    /**
     * 签名key
     */
    private String signKey;

    /**
     * 当前审批人Id
     */
    private long currentOperatorId;

    /**
     * 当前审批人名称
     */
    private String currentOperatorName;

    /**
     * 提交审批人Id
     */
    private long submitOperatorId;

    /**
     * 提交审批人名称
     */
    private String submitOperatorName;

    /**
     * 代替的审批人Id
     */
    private long forwardOperatorId;

    /**
     * 代替的审批人名称
     */
    private String forwardOperatorName;

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
     * 是否被撤销，撤销的数据不能当作待办记录
     */
    private boolean revoked;

    /**
     * 是否抄送
     */
    private boolean notify;

    /**
     * 节点状态 | 待办 0、已办 1
     */
    private int recordState;
    /**
     * 流程状态 | 运行中 0、终止 1 已完成 2、异常 3、删除 4
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
     * 发起者名称
     */
    private String createOperatorName;

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
     * {@link FlowRecord#getTodoKey()}
     */
    private boolean mergeable;
    /**
     * 被干预的用户Id
     */
    private long interferedOperatorId;

    /**
     * 被干预的用户名称
     */
    private String interferedOperatorName;

    /**
     * 委托记录id
     */
    private long delegateId;

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

    /**
     * 数据合并的依据,当开启时值为固定值，否则为随机数据
     * 相同的 {@link FlowRecord#currentOperatorId} {@link FlowRecord#workRuntimeId} {@link FlowRecord#nodeId}字段的数据合并到一条记录上。
     */
    public String getTodoKey() {
        if (mergeable) {
            return String.format("%s-%s-%s", currentOperatorId, workRuntimeId, nodeId);
        } else {
            return String.valueOf(id);
        }
    }


    public FlowRecord(FlowSession flowSession, int nodeOrder) {
        IFlowAction action = flowSession.getCurrentAction();
        // 当前操作者
        IFlowOperator sourceOperator = flowSession.getCurrentOperator();
        // 获取转交之后的审批人
        IFlowOperator currentOperator = flowSession.loadFinalForwardOperator(sourceOperator);
        this.workCode = flowSession.getWorkCode();
        this.workRuntimeId = flowSession.getWorkflowRuntimeId();
        this.workTitle = flowSession.getWorkflow().getTitle();
        this.nodeId = flowSession.getCurrentNodeId();
        this.nodeType = flowSession.getCurrentNodeType();
        this.nodeName = flowSession.getCurrentNodeName();
        this.formData = flowSession.getFormData().toMapData();
        this.nodeOrder = nodeOrder;
        this.processId = RandomUtils.generateStringId();
        this.createOperatorId = flowSession.getCreatedOperator().getUserId();
        this.createOperatorName = flowSession.getCreatedOperator().getName();
        // 是否转交之后的人来审批的判断
        if (currentOperator.equals(sourceOperator)) {
            this.forwardOperatorId = 0;
        } else {
            this.forwardOperatorId = sourceOperator.getUserId();
            this.forwardOperatorName = sourceOperator.getName();
        }
        this.recordState = SATE_RECORD_TODO;
        this.actionId = action.id();
        this.actionType = action.type();
        this.actionName = action.title();

        this.currentOperatorId = currentOperator.getUserId();
        this.currentOperatorName = currentOperator.getName();

        this.submitOperatorId = flowSession.getSubmitOperatorId();
        this.submitOperatorName = flowSession.getSubmitOperatorName();

        this.advice = flowSession.getAdvice().getAdvice();
        this.signKey = flowSession.getAdvice().getSignKey();
        this.flowState = SATE_FLOW_RUNNING;
        this.createTime = System.currentTimeMillis();
        this.hidden = false;

        flowSession.getCurrentNode().fillNewRecord(flowSession.updateSession(currentOperator), this);
        this.extendsRecord(flowSession.getCurrentRecord());

        this.verify();
    }


    /**
     * 继承记录
     *
     * @param record 传递的记录
     */
    public void extendsRecord(FlowRecord record) {
        if (record != null) {
            this.parallelBranchNodeId = record.parallelBranchNodeId;
            this.parallelBranchTotal = record.parallelBranchTotal;
            this.parallelId = record.parallelId;
            this.fromId = record.id;
            this.processId = record.processId;
            this.delegateId = record.delegateId;
            this.parentId = record.getParentId();
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
        return recordState == SATE_RECORD_TODO && flowState == SATE_FLOW_RUNNING && !hidden && !revoked;
    }

    /**
     *  是否已办
     */
    public boolean isDone() {
        return recordState == SATE_RECORD_DONE && !hidden && !revoked;
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
     * @param pass        是否通过
     */
    public void update(FlowSession flowSession, boolean pass) {
        IFlowAction flowAction = flowSession.getCurrentAction();
        FlowAdvice flowAdvice = flowSession.getAdvice();
        this.formData = flowSession.getFormData().toMapData();
        this.actionId = flowAction.id();
        this.actionType = flowAction.type();
        this.actionName = flowAction.title();
        this.readable = true;
        this.readTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.recordState = pass ? SATE_RECORD_DONE : SATE_RECORD_TODO;

        // 设置流程干预人信息，流程干预只能由流程管理员才能操作
        if (flowSession.getCurrentOperator().getUserId() != this.currentOperatorId) {
            this.interferedOperatorId = flowSession.getCurrentOperator().getUserId();
            this.interferedOperatorName = flowSession.getCurrentOperator().getName();
        }

        if (flowAdvice != null) {
            this.advice = flowAdvice.getAdvice();
            this.signKey = flowAdvice.getSignKey();
        }
    }


    /**
     * 清空已办
     */
    public void clearDone() {
        this.readable = true;
        this.readTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.recordState = SATE_RECORD_TODO;
        this.advice = null;
        this.signKey = null;
    }


    /**
     * 流程结束
     */
    public void finish(boolean success) {
        this.flowState = success ? SATE_FLOW_FINISH : SATE_FLOW_DONE;
        this.finishTime = System.currentTimeMillis();
    }


    /**
     * 抄送记录更新
     */
    public void notifyRecord(FlowSession flowSession) {
        IFlowNode currentNode = flowSession.getCurrentNode();
        NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
        this.setTitle(nodeStrategyManager.generateTitle(flowSession));
        this.setTimeoutTime(nodeStrategyManager.getTimeoutTime());
        this.setMergeable(nodeStrategyManager.isEnableMergeable());
        this.update(flowSession, true);
        this.notify = true;
        this.forwardOperatorId = 0;
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

    /**
     * 重置加签节点信息
     */
    public void resetAddAudit(long fromId, int nodeOrder, long currentOperatorId, boolean hidden) {
        this.fromId = fromId;
        this.nodeOrder = nodeOrder;
        this.currentOperatorId = currentOperatorId;
        this.hidden = hidden;
    }

    public FlowRecord create(FlowSession flowSession) {
        FlowRecord flowRecord = new FlowRecord(flowSession, 0);
        flowRecord.currentOperatorId = flowSession.getCurrentOperator().getUserId();
        flowRecord.currentOperatorName = flowSession.getCurrentOperator().getName();
        return flowRecord;
    }

    /**
     * 重置委托节点信息
     */
    public void resetDelegate(FlowRecord currentRecord) {
        this.delegateId = currentRecord.id;
    }

    /**
     * 判断是否委托
     */
    public boolean isDelegate() {
        return delegateId > 0;
    }

    /**
     * 清空委托节点信息
     */
    public void clearDelegate() {
        this.delegateId = 0;
    }

    /**
     * 撤销
     */
    public void revoke() {
        this.revoked = true;
    }


    /**
     * 设置为新的记录
     */
    public void newRecord() {
        this.setAdvice(null);
        this.setActionId(null);
    }

    /**
     * 判断是否转交记录
     */
    public boolean isForward() {
        return forwardOperatorId > 0;
    }


    /**
     * 创建会话
     * @param repositoryHolder 资源持有者
     * @param workflow 流程设计器
     * @param currentOperator 当前操作人
     * @param formData 表单数据
     * @param advice 节点审批信息
     * @return FlowSession
     */
    public FlowSession createFlowSession(IRepositoryHolder repositoryHolder,
                                         Workflow workflow,
                                         IFlowOperator currentOperator,
                                         IFlowOperator createdOperator,
                                         IFlowOperator submitOperator,
                                         FormData formData,
                                         FlowAdvice advice) {
        List<FlowRecord> currentRecords = repositoryHolder.findCurrentNodeRecords(this.getFromId(), this.getNodeId());
        IFlowNode currentNode = workflow.getFlowNode(nodeId);
        return new FlowSession(
                repositoryHolder,
                currentOperator,
                createdOperator,
                submitOperator,
                workflow,
                currentNode,
                advice.getAction(),
                formData,
                this,
                currentRecords,
                this.workRuntimeId,
                advice
        );
    }

    /**
     * 设置为已读
     */
    public void read() {
        this.readTime = System.currentTimeMillis();
    }

    /**
     * 流程结束
     */
    public void over() {
        this.title = "-";
        this.readTime = System.currentTimeMillis();
        this.currentOperatorId = -1;
        this.recordState = SATE_RECORD_DONE;
    }

    /**
     *  判断是否结束节点的记录
     */
    public boolean isNotEndNode() {
        return !EndNode.NODE_TYPE.equals(this.nodeType);
    }
}
