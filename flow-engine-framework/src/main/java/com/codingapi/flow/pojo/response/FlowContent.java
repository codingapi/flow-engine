package com.codingapi.flow.pojo.response;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.manager.ActionManager;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.workflow.Workflow;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程详情
 */
@Data
public class FlowContent {

    /**
     * 流程记录编号
     */
    private long recordId;

    /**
     * 流程id
     * 每一次流程启动时生成，直到流程结束
     */
    private String processId;

    /**
     * 流程编号
     */
    private String workId;

    /**
     * 流程设计名称
     */
    private String workTitle;

    /**
     * 流程设计备注
     */
    private String workDescription;

    /**
     * 流程创建时间
     */
    private long createTime;

    /**
     * 流程编码
     */
    private String workCode;
    /**
     * 流程视图
     */
    private String view;

    /**
     * 视图代码
     */
    private String code;

    /**
     * 视图标题
     */
    private String viewTitle;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点Id
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 流程标题
     */
    private String title;

    /**
     * 审批意见是否必填
     */
    private boolean adviceRequired;

    /**
     * 签名是否必填
     */
    private boolean signRequired;

    /**
     * 是否隐藏审批意见，开启后审批时不展示审批意见输入框
     */
    private boolean adviceHidden;

    /**
     * 表单元数据
     */
    private FlowForm form;

    /**
     * 表单字段权限
     */
    private List<FormFieldPermission> fieldPermissions;
    /**
     * 流程记录
     */
    private List<Body> todos;

    /**
     * 展示流程按钮
     */
    private List<Map<String, Object>> actions;

    /**
     * 全部展示按钮
     */
    private List<Map<String, Object>> actionList;

    /**
     * 是否可合并
     */
    private boolean mergeable;

    /**
     * 流程发起者
     */
    private FlowOperator createOperator;

    /**
     * 当前审批者
     */
    private FlowOperator currentOperator;

    /**
     * 流程状态 | 运行中、已完成、异常、删除
     */
    private int flowState;

    /**
     * 节点状态 | 待办、已办
     */
    private int recordState;

    /**
     * 历史记录
     */
    private List<History> histories;

    /**
     * 所有节点
     */
    private List<NodeOption> nodes;

    /**
     * 支持撤销
     */
    private boolean revoke;

    /**
     * 支持催办
     */
    private boolean urge;

    /**
     * 支持子流程数据重置（独立能力标识，非审批动作）。
     * <p>当前待办记录位于已开启重置能力、且已汇聚完成的子流程下游时为 true；
     * 前端可据此标识自行决定交互呈现方式，调用 {@code /api/cmd/record/subProcess/reset} 接口。</p>
     */
    private boolean resetSubProcess;

    /**
     * 设置操作动作按钮
     *
     */
    public void setOperationAction(Workflow workflow, FlowRecord flowRecord) {
        if (flowRecord == null) {
            this.revoke = false;
            this.urge = false;
        } else {
            if (flowRecord.isFinish()) {
                this.revoke = false;
                this.urge = false;
            }

            if (flowRecord.isTodo()) {
                this.revoke = false;
                this.urge = false;
            }

            // 自动办结记录（相同人员自动通过 / 或签并签遗留待办的自动已办）未发生真实审批，
            // 不暴露撤销/催办入口，与服务端 FlowRevokeService 的 isAutoDone 拦截保持一致（issue #226）
            if (flowRecord.isDone() && !flowRecord.isFinish() && !flowRecord.isAutoDone()) {
                IFlowNode node = workflow.getFlowNode(flowRecord.getNodeId());
                this.revoke = node.strategyManager().isEnableRevoke();
                this.urge = workflow.strategyManager().isEnableUrge();
            }
        }
    }

    public void pushCurrentNode(FlowSession flowSession) {
        IFlowNode currentNode = flowSession.getCurrentNode();
        if (currentNode != null) {
            ActionManager actionManager = currentNode.actionManager();
            NodeStrategyManager strategyManager = currentNode.strategyManager();
            this.actions = actionManager.filterActions(flowSession).stream().map(IFlowAction::toMap).toList();
            this.actionList = actionManager.getActions().stream().map(IFlowAction::toMap).toList();
            this.adviceRequired = strategyManager.isAdviceRequired();
            this.signRequired = strategyManager.isSignRequired();
            this.adviceHidden = strategyManager.isAdviceHidden();
            this.nodeId = currentNode.getId();
            this.nodeName = currentNode.getName();
            this.nodeType = currentNode.getType();
            this.fieldPermissions = strategyManager.getFieldPermissions();
            Map<String, Object> nodeData = currentNode.toMap();
            this.view = (String) nodeData.get("view");
            this.code = (String) nodeData.get("code");
            this.viewTitle = (String) nodeData.get("viewTitle");
        }
    }

    public void pushWorkflow(Workflow workflow) {
        this.form = workflow.getForm();
        this.workCode = workflow.getCode();
        this.workId = workflow.getId();
        this.workTitle = workflow.getTitle();
        this.workDescription = workflow.getDescription();
        this.nodes = workflow.getNodes().stream().map(NodeOption::new).toList();
    }

    public void pushRecords(FlowRecord record, List<FlowRecord> mergeRecords) {
        this.recordId = record.getId();
        this.processId = record.getProcessId();
        this.createOperator = new FlowOperator(record.getCreateOperatorId(), record.getCreateOperatorName());
        this.mergeable = record.isTodo() && record.isMergeable();
        this.flowState = record.getFlowState();
        this.recordState = record.getRecordState();
        this.title = record.getTitle();
        this.createTime = record.getCreateTime();

        this.todos = new ArrayList<>();
        for (FlowRecord item : mergeRecords) {
            Body body = new Body();
            body.setRecordId(item.getId());
            body.setProcessId(item.getProcessId());
            body.setWorkTitle(item.getWorkTitle());
            body.setNodeId(item.getNodeId());
            body.setNodeName(item.getNodeName());
            body.setNodeType(item.getNodeType());
            body.setSubmitOperator(new FlowOperator(item.getSubmitOperatorId(), item.getSubmitOperatorName()));
            body.setCreatedOperator(new FlowOperator(item.getCreateOperatorId(), item.getCreateOperatorName()));
            body.setTitle(item.getTitle());
            body.setData(item.getFormData());
            body.setRecordState(item.getRecordState());
            body.setFlowState(item.getFlowState());
            body.setCreateTime(item.getCreateTime());
            this.todos.add(body);
        }
    }

    public void pushHistory(Workflow workflow, List<FlowRecord> historyRecords) {
        this.histories = new ArrayList<>();
        for (FlowRecord item : historyRecords) {
            IFlowNode node = workflow.getFlowNode(item.getNodeId());
            History history = new History();
            history.setRecordId(item.getId());
            history.setTitle(item.getTitle());
            history.setAdvice(item.getAdvice());
            history.setSignKey(item.getSignKey());
            history.setNodeName(node.getName());
            history.setNodeId(item.getNodeId());
            history.setNodeType(item.getNodeType());
            history.setUpdateTime(item.getUpdateTime());
            history.setCurrentOperator(new FlowOperator(item.getCurrentOperatorId(), item.getCurrentOperatorName()));
            this.histories.add(history);
        }
    }

    public void pushCurrentOperator(IFlowOperator currentOperator) {
        this.currentOperator = new FlowOperator(currentOperator);
        this.createOperator = new FlowOperator(currentOperator);
    }

    @Data
    public static class History {
        /**
         * 流程编号
         */
        private long recordId;
        /**
         * 流程标题
         */
        private String title;

        /**
         * 审批意见
         */
        private String advice;

        /**
         * 签名key
         */
        private String signKey;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 节点id
         */
        private String nodeId;
        /**
         * 节点类型
         */
        private String nodeType;

        /**
         * 更新时间
         */
        private long updateTime;

        /**
         * 当前审批人
         */
        private FlowOperator currentOperator;
    }

    @Data
    public static class Body {
        /**
         * 流程记录编号
         */
        private long recordId;

        /**
         * 流程id
         * 每一次流程启动时生成，直到流程结束
         */
        private String processId;

        /**
         * 流程设计名称
         */
        private String workTitle;

        /**
         * 节点Id
         */
        private String nodeId;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 节点类型
         */
        private String nodeType;
        /**
         * 流程标题
         */
        private String title;

        /**
         * 流程提交人
         */
        private FlowOperator submitOperator;

        /**
         * 流程创建者
         */
        private FlowOperator createdOperator;

        /**
         * 流程创建时间
         */
        private long createTime;

        /**
         * 表单数据
         */
        private Map<String, Object> data;

        /**
         * 节点状态 | 待办、已办
         */
        private int recordState;
        /**
         * 流程状态 | 运行中、已完成、异常、删除
         */
        private int flowState;
    }
}
