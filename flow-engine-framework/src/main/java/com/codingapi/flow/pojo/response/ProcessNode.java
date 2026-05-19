package com.codingapi.flow.pojo.response;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.strategy.node.OperatorSelectType;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
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
        APPROVER_SELECT,

        /**
         *  无人员设置
         */
        NO_OPERATOR
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

    private void resetApproveState(FlowRecord flowRecord) {
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

    @Getter
    @AllArgsConstructor
    public static class FlowRecordOperator {
        private final FlowRecord flowRecord;
        private final IFlowOperator flowOperator;
    }


    public static ProcessNode createByRecord(List<FlowRecordOperator> recordOperatorList, Workflow workflow) {

        FlowRecord currentRecord = null;
        for (FlowRecordOperator flowRecordOperator : recordOperatorList) {
            if (flowRecordOperator.getFlowRecord().isTodo()) {
                currentRecord = flowRecordOperator.getFlowRecord();
            }
        }

        if (currentRecord == null) {
            currentRecord = recordOperatorList.get(0).getFlowRecord();
        }


        IFlowNode flowNode = workflow.getFlowNode(currentRecord.getNodeId());

        ProcessNode processNode = new ProcessNode();
        processNode.setId(String.valueOf(currentRecord.getId()));
        processNode.setNodeId(flowNode.getId());
        processNode.setNodeName(flowNode.getName());
        processNode.setNodeType(flowNode.getType());
        processNode.resetApproveState(currentRecord);
        processNode.resetApproveStrategy(flowNode);
        processNode.setOperatorStrategy(OperatorStrategy.OPERATOR_LIST);

        List<FlowOperatorBody> flowOperatorBodyList = new ArrayList<>();
        for (FlowRecordOperator flowOperator : recordOperatorList) {
            flowOperatorBodyList.add(new FlowOperatorBody(flowOperator.getFlowRecord(), flowOperator.getFlowOperator()));
        }
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

        OperatorStrategy operatorStrategy = OperatorStrategy.NO_OPERATOR;

        if (operators != null && !operators.isEmpty()) {
            List<FlowOperatorBody> flowOperatorBodyList = new ArrayList<>();
            for (IFlowOperator operator : operators) {
                flowOperatorBodyList.add(new FlowOperatorBody(operator));
            }
            processNode.setOperators(flowOperatorBodyList);
            operatorStrategy = OperatorStrategy.OPERATOR_LIST;
        } else {
            if (operatorSelectType == OperatorSelectType.APPROVER_SELECT) {
                operatorStrategy = OperatorStrategy.APPROVER_SELECT;
            }
            if (operatorSelectType == OperatorSelectType.INITIATOR_SELECT) {
                operatorStrategy = OperatorStrategy.INITIATOR_SELECT;
            }
        }
        processNode.setOperatorStrategy(operatorStrategy);
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
        private IFlowOperator flowOperator;
        /**
         * 审批时间
         */
        private long approveTime;

        public FlowOperatorBody(FlowRecord flowRecord, IFlowOperator flowOperator) {
            this.advice = flowRecord.getAdvice();
            this.signKey = flowRecord.getSignKey();
            this.approveTime = flowRecord.getUpdateTime();
            this.actionName = flowRecord.getActionName();
            this.actionType = flowRecord.getActionType();
            this.flowOperator = flowOperator;
        }

        public FlowOperatorBody(IFlowOperator flowOperator) {
            this.flowOperator = flowOperator;
        }

    }

}
