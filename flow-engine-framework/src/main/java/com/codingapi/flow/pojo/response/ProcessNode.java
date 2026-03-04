package com.codingapi.flow.pojo.response;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.workflow.Workflow;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 流程审批节点
 */
@Data
@NoArgsConstructor
public class ProcessNode {

    public final static int STATE_HISTORY = -1;
    public final static int STATE_CURRENT = 0;
    public final static int STATE_NEXT = 1;

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
     * 记录状态
     * -1 为历史状态
     * 0 为当前状态
     * 1 为后续状态
     */
    private int state;

    /**
     * 节点审批人
     */
    private List<FlowOperatorBody> operators;


    public boolean isHistory(){
        return this.state == STATE_HISTORY;
    }

    public ProcessNode(FlowRecord flowRecord, Workflow workflow) {
        this.nodeId = flowRecord.getNodeId();
        IFlowNode flowNode = workflow.getFlowNode(this.nodeId);
        this.nodeName = flowNode.getName();
        this.nodeType = flowNode.getType();
        this.operators = new ArrayList<>();
        this.state = STATE_HISTORY;
        this.operators.add(new FlowOperatorBody(flowRecord));
    }


    public ProcessNode(IFlowNode flowNode, List<IFlowOperator> operators) {
        this.nodeId = flowNode.getId();
        this.nodeName = flowNode.getName();
        this.nodeType = flowNode.getType();
        this.operators = operators.stream().map(FlowOperatorBody::new).toList();
        this.state = STATE_NEXT;
    }


    public boolean isFlowNode(IFlowNode currentNode) {
        return this.nodeId.equals(currentNode.getId());
    }

    public void setCurrentState() {
        this.state = STATE_CURRENT;
    }


    @Override
    public boolean equals(Object target) {
        if(target instanceof ProcessNode) {
            ProcessNode targetNode = (ProcessNode) target;
            return targetNode.getNodeId().equals(this.getNodeId());
        }
        return super.equals(target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeName, nodeType, state, operators);
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
         * 审批记录
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
            this.flowOperator = new FlowOperator(flowRecord.getCurrentOperatorId(), flowRecord.getCurrentOperatorName());
        }

        public FlowOperatorBody(IFlowOperator flowOperator) {
            this.flowOperator = new FlowOperator(flowOperator);
        }

    }

}
