package com.codingapi.flow.pojo.response;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
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
     * 流程编号
     */
    private String workId;

    /**
     * 流程编码
     */
    private String workCode;
    /**
     * 流程视图
     */
    private String view;

    /**
     * 表单元数据
     */
    private FormMeta form;
    /**
     * 流程记录
     */
    private List<Body> todos;

    /**
     * 流程按钮
     */
    private List<IFlowAction> actions;

    /**
     * 是否可合并
     */
    private boolean mergeable;

    /**
     * 发起者id
     */
    private FlowOperator createOperator;

    /**
     * 当前审批者id
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
     * 下一审批
     */
    private List<NextNode> nextNodes;

    public void pushNextNodes(FlowSession flowSession, List<IFlowNode> nextNodes) {
        List<NextNode> nextNodeList = new ArrayList<>();
        for (IFlowNode node : nextNodes){
            NextNode nextNode = new NextNode();
            nextNode.setNodeId(node.getId());
            nextNode.setNodeName(node.getName());
            nextNode.setNodeType(node.getType());

            NodeStrategyManager nodeStrategyManager = node.strategyManager();
            OperatorManager operatorManager = nodeStrategyManager.loadOperators(flowSession);
            nextNode.setOperators(operatorManager.getOperators().stream().map(FlowOperator::new).toList());

            nextNodeList.add(nextNode);
        }
        this.nextNodes = nextNodeList;
    }

    public void pushCurrentNode(IFlowNode currentNode) {
        this.actions = currentNode.actionManager().getActions();
        Map<String,Object> nodeData = currentNode.toMap();
        this.view = (String) nodeData.get("view");
    }

    public void pushWorkflow(Workflow workflow) {
        this.form = workflow.getForm();
        this.workCode = workflow.getCode();
        this.workId = workflow.getId();
    }

    public void pushRecords(FlowRecord record, List<FlowRecord> mergeRecords) {
        this.recordId = record.getId();
        this.createOperator = new FlowOperator(record.getCreateOperatorId(), record.getCreateOperatorName());
        this.mergeable = record.isMergeable();
        this.flowState = record.getFlowState();
        this.recordState = record.getRecordState();

        this.todos = new ArrayList<>();
        for (FlowRecord item : mergeRecords){
            Body body = new Body();
            body.setRecordId(item.getId());
            body.setTitle(item.getTitle());
            body.setData(item.getFormData());
            body.setRecordState(item.getRecordState());
            body.setFlowState(item.getFlowState());
            this.todos.add(body);
        }
    }

    public void pushHistory(Workflow workflow,List<FlowRecord> historyRecords) {
        this.histories = new ArrayList<>();
        for (FlowRecord item : historyRecords){
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
        this.createOperator =  new FlowOperator(currentOperator);
    }


    /**
     * 流程图
     */
    @Data
    public static class NextNode{
        /**
         * 节点名称
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
         * 节点审批人
         */
        private List<FlowOperator> operators;
    }

    @Data
    public static class History{
        /**
         * 流程编号
         */
        private long recordId;
        /**
         *  流程标题
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
         *  流程记录编号
         */
        private long recordId;
        /**
         *  流程标题
         */
        private String title;
        /**
         *  表单数据
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
