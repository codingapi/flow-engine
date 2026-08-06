package com.codingapi.flow.node;

import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.javscript.annotation.NodeViewScript;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@NoArgsConstructor
public abstract class BaseAuditNode extends BaseFlowNode implements IFlowNode {

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;

    /**
     * 视图代码
     */
    @Getter
    @Setter
    @NodeViewScript
    private String code;

    /**
     * 视图标题
     */
    @Getter
    @Setter
    private String viewTitle;

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("view", view);
        map.put("code", code);
        map.put("viewTitle", viewTitle);
        return map;
    }


    public void verifyNode(FlowForm form) {
        super.verifyNode(form);
        if (!StringUtils.hasText(view)) {
            throw FlowValidationException.nodeRequired("view");
        }
        if (!StringUtils.hasText(code)) {
            throw FlowValidationException.nodeRequired("code");
        }
    }


    @Override
    public boolean handle(FlowSession session) {
        return false;
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        flowRecord.setTitle(nodeStrategyManager.generateTitle(session));
        flowRecord.setTimeoutTime(nodeStrategyManager.getTimeoutTime());
        flowRecord.setMergeable(nodeStrategyManager.isEnableMergeable());
        flowRecord.newRecord();
    }

    @Override
    public boolean isFinish(FlowSession session) {
        List<FlowRecord> currentRecords = session.getCurrentNodeRecords();
        FlowRecord currentRecord = session.getCurrentRecord();
        // 多人审批
        if (currentRecords.size() > 1) {
            NodeStrategyManager nodeStrategyManager = this.strategyManager();
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = nodeStrategyManager.getMultiOperatorAuditStrategyType();
            // 顺序审批
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.SEQUENCE) {
                int currentOrder = currentRecord.getNodeOrder();
                int maxNodeOrder = currentRecords.size() - 1;
                return currentOrder >= maxNodeOrder;
            }
            // 或签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.ANY) {
                return true;
            }
            // 并签
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.MERGE) {
                float percent = nodeStrategyManager.getMultiOperatorAuditMergePercent();
                long total = currentRecords.size();
                // 尚未办理的数量为所有待办数-1，1是当前办理的这条记录
                long todoCount = currentRecords.stream().filter(FlowRecord::isTodo).count() - 1;
                long doneCount = total - todoCount;
                return doneCount >= total * percent;
            }
        }
        return true;
    }


    /**
     * 生成当前节点的记录
     *
     * @param session 触发会话
     * @return 生成当前节点的记录
     */
    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        return this.generateCurrentRecords(session, new LinkedHashSet<>(), 0);
    }

    /**
     * 生成当前节点的记录。
     * <p>
     * 带本次生成链的已访问节点集合与递归深度，防护异常触发（errorTrigger）跳回已访问节点
     * 或超深跳转导致的无限递归。
     *
     * @param session         触发会话
     * @param visitedNodeIds  本次记录生成过程中已访问的节点id
     * @param depth           本次 errorTrigger 递归深度
     * @return 生成当前节点的记录
     */
    private List<FlowRecord> generateCurrentRecords(FlowSession session, Set<String> visitedNodeIds, int depth) {

        // 节点执行次数兜底（环形状：同一节点在实例链上反复执行）
        this.verifyNodeExecutionCount(session);

        // 是否等待并行合并节点
        if (this.isWaitRecordMargeParallelNode(session)) {
            return List.of();
        }

        List<FlowRecord> records = new ArrayList<>();
        NodeStrategyManager nodeStrategyManager = this.strategyManager();
        OperatorManager operatorManager = nodeStrategyManager.loadOperators(session);
        // 执行异常节点配置
        if(operatorManager.isEmpty()){
            ErrorThrow errorThrow =  nodeStrategyManager.errorTrigger(session);
            if(errorThrow==null){
                throw FlowValidationException.nodeRequired("errorTrigger");
            }
            if(errorThrow.isNode()){
                IFlowNode errorNode = errorThrow.getNode();
                // 抄送/子流程节点不产生有效记录，跳转会静默停滞
                verifyJumpTarget(errorNode);
                if (!visitedNodeIds.add(errorNode.getId())) {
                    throw FlowExecutionException.errorTriggerLoop();
                }
                int maxNestDepth = session.getWorkflow().getMaxNestDepth();
                if (depth + 1 > maxNestDepth) {
                    throw FlowExecutionException.errorTriggerDepthExceeded(maxNestDepth);
                }
                FlowSession errorSession = session.updateSession(errorNode);
                if (errorNode instanceof BaseAuditNode auditNode) {
                    return auditNode.generateCurrentRecords(errorSession, visitedNodeIds, depth + 1);
                }
                return errorNode.generateCurrentRecords(errorSession);
            }else {
                operatorManager = new OperatorManager(errorThrow.getOperators());
            }
        }
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), order);
            flowRecord.cleanAction();
            records.add(flowRecord);
        }
        if (operators.size() > 1) {
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = nodeStrategyManager.getMultiOperatorAuditStrategyType();
            // 如果是顺序审批，则隐藏掉后续的人员的审批记录
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.SEQUENCE) {
                for (int i = 1; i < records.size(); i++) {
                    FlowRecord record = records.get(i);
                    record.hidden();
                }
            }
            // 如果是随机审批，则隐藏掉后续的人员的审批记录
            if (multiOperatorAuditStrategyType == MultiOperatorAuditStrategy.Type.RANDOM_ONE) {
                Random random = new Random();
                int index = random.nextInt(operators.size());

                List<FlowRecord> newRecords = new ArrayList<>();
                for (FlowRecord record : records) {
                    if (record.getNodeOrder() == index) {
                        record.resetNodeOrder(0);
                        newRecords.add(record);
                    }
                }
                return newRecords;
            }
        }

        return records;
    }

    /**
     * 校验当前节点在流程实例链上的执行次数。
     * <p>
     * 同一节点在实例链上反复执行（如抄送节点参与的正向流转环 B -> C -> B）会不断产生记录，
     * 超过流程级 {@link com.codingapi.flow.workflow.Workflow#getMaxNestDepth()} 时拒绝继续，
     * 作为环形状循环的最后兜底。
     *
     * @param session 触发会话
     */
    protected void verifyNodeExecutionCount(FlowSession session) {
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord == null) {
            return;
        }
        int maxNestDepth = session.getWorkflow().getMaxNestDepth();
        long executedCount = session.getRepositoryHolder()
                .findProcessRecords(currentRecord.getProcessId()).stream()
                .filter(record -> record.getNodeId().equals(session.getCurrentNodeId()))
                .count();
        if (executedCount + 1 > maxNestDepth) {
            throw FlowExecutionException.nodeLoopDepthExceeded(maxNestDepth);
        }
    }


    @SneakyThrows
    public static <T extends BaseAuditNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = BaseFlowNode.fromMap(map, clazz);
        node.setView((String) map.get("view"));
        node.setCode((String) map.get("code"));
        node.setViewTitle((String) map.get("viewTitle"));
        return node;
    }

}
