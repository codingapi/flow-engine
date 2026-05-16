package com.codingapi.flow.pojo.response;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.flow.workflow.Workflow;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程审批节点
 */
@Data
@NoArgsConstructor
public class ProcessNode {

    /**
     * 记录id
     */
    private String id;

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
     * 是否呈现节点
     */
    private MultiOperatorAuditStrategy.Type approveStrategy;

    /**
     * 审批状态
     */
    private ApproveState approveState;

    /**
     * 人员模式
     */
    private OperatorStrategy operatorStrategy;

    /**
     * 节点审批人
     */
    private List<FlowOperatorBody> operators;

    public boolean isHistory() {
        return this.approveState == ApproveState.PASS || this.approveState == ApproveState.ERROR;
    }

    public void addFlowRecordOperator(FlowRecord record) {
        if (this.operators == null) {
            this.operators = new ArrayList<>();
        }
        this.operators.add(new FlowOperatorBody(record));
    }

    public enum OperatorStrategy {
        /**
         * 指定人员
         */
        OPERATOR_LIST,
        /**
         * 发起人设定：流程创建时由发起人为该节点指定操作人
         */
        INITIATOR_SELECT,

        /**
         * 审批人设定：当前节点审批时，审批人为下游该节点指定操作人
         */
        APPROVER_SELECT
    }

    public enum ApproveState {
        // 审批通过
        PASS,
        // 审批中
        PROCESSING,
        // 未审批
        PENDING,
        // 审批错误
        ERROR
    }

    public void resetApproveState(FlowRecord flowRecord) {
        if (flowRecord.isDone()) {
            this.approveState = ApproveState.PASS;
        }

        if (flowRecord.isError()) {
            this.approveState = ApproveState.ERROR;
        }

        if (flowRecord.isHidden()) {
            this.approveState = ApproveState.PROCESSING;
        }

        if (flowRecord.isTodo()) {
            this.approveState = ApproveState.PROCESSING;
        }
    }

    private void resetApproveStrategy(IFlowNode flowNode) {
        MultiOperatorAuditStrategy.Type type = flowNode.strategyManager().getMultiOperatorAuditStrategyType();
        if (type != null) {
            this.setApproveStrategy(type);
        } else {
            this.setApproveStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE);
        }
    }


    public static ProcessNode createByRecord(FlowRecord flowRecord, Workflow workflow) {
        IFlowNode flowNode = workflow.getFlowNode(flowRecord.getNodeId());

        ProcessNode processNode = new ProcessNode();
        processNode.setId(String.valueOf(flowRecord.getId()));
        processNode.setNodeId(flowNode.getId());
        processNode.setNodeName(flowNode.getName());
        processNode.setNodeType(flowNode.getType());
        processNode.resetApproveState(flowRecord);
        processNode.resetApproveStrategy(flowNode);
        processNode.setOperatorStrategy(OperatorStrategy.OPERATOR_LIST);

        List<FlowOperatorBody> flowOperatorBodyList = new ArrayList<>();
        flowOperatorBodyList.add(new FlowOperatorBody(flowRecord));
        processNode.setOperators(flowOperatorBodyList);

        return processNode;
    }


    public static ProcessNode createByEndNode(IFlowNode flowNode, boolean finish) {
        ProcessNode processNode = new ProcessNode();
        processNode.setId(flowNode.getId());
        processNode.setNodeId(flowNode.getId());
        processNode.setNodeName(flowNode.getName());
        processNode.setNodeType(flowNode.getType());
        processNode.setApproveState(finish ? ApproveState.PASS : ApproveState.PENDING);
        processNode.setApproveStrategy(MultiOperatorAuditStrategy.Type.SEQUENCE);
        processNode.setOperatorStrategy(OperatorStrategy.OPERATOR_LIST);
        return processNode;
    }

    public static ProcessNode createByNode(IFlowNode flowNode, OperatorSelectType operatorSelectType, List<IFlowOperator> operators) {
        ProcessNode processNode = new ProcessNode();
        processNode.setId(flowNode.getId());
        processNode.setNodeId(flowNode.getId());
        processNode.setNodeName(flowNode.getName());
        processNode.setNodeType(flowNode.getType());
        processNode.setApproveState(ApproveState.PENDING);
        processNode.resetApproveStrategy(flowNode);

        if (operators != null && !operators.isEmpty()) {
            List<FlowOperatorBody> flowOperatorBodyList = new ArrayList<>();
            for (IFlowOperator operator : operators) {
                flowOperatorBodyList.add(new FlowOperatorBody(operator));
            }
            processNode.setOperators(flowOperatorBodyList);
            processNode.setOperatorStrategy(OperatorStrategy.OPERATOR_LIST);
        } else {
            if (operatorSelectType == OperatorSelectType.APPROVER_SELECT) {
                processNode.setOperatorStrategy(OperatorStrategy.APPROVER_SELECT);
            }
            if (operatorSelectType == OperatorSelectType.INITIATOR_SELECT) {
                processNode.setOperatorStrategy(OperatorStrategy.INITIATOR_SELECT);
            }
        }

        return processNode;
    }

    /**
     * 审批意见内容，仅当历史节点存在数据
     */
    @Data
    @NoArgsConstructor
    public static class FlowOperatorBody {

        /**
         * 审批意见
         */
        private String advice;

        /**
         * 签名key
         */
        private String signKey;

        /**
         * 审批类型
         */
        private String actionType;

        /**
         * 审批动作
         */
        private String actionName;
        /**
         * 审批人
         */
        private FlowOperator flowOperator;
        /**
         * 审批时间
         */
        private long approveTime;

        public FlowOperatorBody(FlowRecord flowRecord) {
            this.advice = flowRecord.getAdvice();
            this.signKey = flowRecord.getSignKey();
            this.approveTime = flowRecord.getCreateTime();
            this.actionName = flowRecord.getActionName();
            this.flowOperator = new FlowOperator(flowRecord.getCurrentOperatorId(), flowRecord.getCurrentOperatorName());
        }

        public FlowOperatorBody(IFlowOperator flowOperator) {
            this.flowOperator = new FlowOperator(flowOperator);
        }

    }

}
