package com.codingapi.flow.pojo.response;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.StartNode;
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
import java.util.function.Function;

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

    /**
     * 是否为查询子流程节点记录时拼接的主流程历史节点。
     */
    private boolean parentProcessRecord;

    /**
     * 子流程节点执行信息，仅 SUB_PROCESS 节点产生执行记录后返回。
     */
    private SubProcessBody subProcess;

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

    public static ProcessNode createBySubProcessRecord(SubProcessRecord record, Workflow workflow) {
        return createBySubProcessRecord(record, workflow, startRecordId -> null);
    }

    public static ProcessNode createBySubProcessRecord(SubProcessRecord record,
                                                       Workflow workflow,
                                                       Function<Long, String> workTitleLoader) {
        IFlowNode flowNode = workflow.getFlowNode(record.getNodeId());
        ProcessNode processNode = new ProcessNode();
        processNode.setId("sub-process:" + record.getId());
        processNode.setNodeId(flowNode.getId());
        processNode.setNodeName(flowNode.getName());
        processNode.setNodeType(flowNode.getType());
        processNode.setApproveState(switch (record.getState()) {
            case WAITING -> ApproveState.PROCESSING;
            case PASSED -> ApproveState.PASS;
            case ERROR -> ApproveState.ERROR;
        });
        processNode.resetApproveStrategy(flowNode);
        processNode.setOperatorStrategy(OperatorStrategy.NO_OPERATOR);
        processNode.setOperators(List.of());
        processNode.setSubProcess(SubProcessBody.create(record, workTitleLoader));
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
        processNode.setApproveState(flowNode instanceof StartNode?ApproveState.PROCESSING:ApproveState.PENDING);
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

        /**
         * 读取时间
         */
        private long readTime;

        /**
         * 是否自动跳过（未实际审批）。
         * <p>多人审批（或签/并签）场景下，节点完成后其他候选人的待办被自动置为已办，
         * 未发生实际审批动作。由后端依据 {@link FlowRecord#isAutoDone()} 判定，
         * 前端据此展示"自动跳过"，无需自行推断。
         */
        private boolean autoSkip;

        public FlowOperatorBody(FlowRecord flowRecord, IFlowOperator flowOperator) {
            this.advice = flowRecord.getAdvice();
            this.signKey = flowRecord.getSignKey();
            this.approveTime = flowRecord.getUpdateTime();
            this.actionName = flowRecord.getActionName();
            this.actionType = flowRecord.getActionType();
            this.flowOperator = flowOperator;
            this.readTime = flowRecord.getReadTime();
            this.autoSkip = flowRecord.isAutoDone();
        }

        public FlowOperatorBody(IFlowOperator flowOperator) {
            this.flowOperator = flowOperator;
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubProcessBody {
        private long recordId;
        private String groupId;
        private long parentRecordId;
        private int totalCount;
        private int finishedCount;
        private SubProcessRecord.State state;
        private long createTime;
        private long finishTime;
        private List<SubProcessInstanceBody> instances;

        private static SubProcessBody create(SubProcessRecord record, Function<Long, String> workTitleLoader) {
            List<SubProcessInstanceBody> instances = record.getInstances().stream()
                    .map(instance -> SubProcessInstanceBody.create(instance, workTitleLoader))
                    .toList();
            int finishedCount = (int) record.getInstances().stream()
                    .filter(SubProcessRecord.Instance::isFinished)
                    .count();
            return new SubProcessBody(
                    record.getId(),
                    record.getGroupId(),
                    record.getParentRecordId(),
                    record.getTotalCount(),
                    finishedCount,
                    record.getState(),
                    record.getCreateTime(),
                    record.getFinishTime(),
                    instances
            );
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubProcessInstanceBody {
        private long startRecordId;
        private String processId;
        private String workTitle;
        private long finishRecordId;
        private SubProcessRecord.InstanceState state;
        private long finishTime;

        private static SubProcessInstanceBody create(SubProcessRecord.Instance instance,
                                                     Function<Long, String> workTitleLoader) {
            String workTitle = instance.getWorkTitle();
            if (workTitle == null || workTitle.isBlank()) {
                workTitle = workTitleLoader.apply(instance.getStartRecordId());
            }
            return new SubProcessInstanceBody(
                    instance.getStartRecordId(),
                    instance.getProcessId(),
                    workTitle,
                    instance.getFinishRecordId(),
                    instance.getState(),
                    instance.getFinishTime()
            );
        }
    }

}
