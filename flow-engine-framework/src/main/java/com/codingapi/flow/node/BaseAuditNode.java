package com.codingapi.flow.node;

import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.context.LoopTriggerTraceContext;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.event.FlowRecordDoneEvent;
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
import com.codingapi.springboot.framework.event.EventPusher;
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
        flowRecord.setMergeType(nodeStrategyManager.getMergeType());
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
        // 提交人与审批人一致时自动审批（issue #224）：提交人流转到该节点且审批人含提交人本人时，
        // 过滤掉与提交人一致的操作员，避免本人审批本人提交的节点（AUTO_PASS 自动通过 / MANUAL_PASS 不跳过）。
        // 守卫条件（currentOperator == submitOperator）用于区分正常流转与加签等"为他人新增记录"的调用路径；
        // 动作类型限定为 PASS（issue #226 评审）：仅正向通过触发本机制，退回（ReturnAction）、拒绝
        // （RejectAction）、加签（AddAuditAction）等路径保持普通记录生成契约——它们的消费方按"返回的
        // 即当前节点待办记录"处理（resetAddAudit、先清后存等），自动通过的留痕记录落库/事件副作用会破坏其语义。
        if (nodeStrategyManager.isSameOperatorAutoPass()
                && session.getCurrentOperator() != null
                && session.getCurrentOperator().getUserId() == session.getSubmitOperatorId()
                && session.getCurrentAction() != null
                && ActionType.PASS.name().equalsIgnoreCase(session.getCurrentAction().type())) {
            long submitOperatorId = session.getSubmitOperatorId();
            operators = operators.stream()
                    .filter(operator -> operator.getUserId() != submitOperatorId)
                    .toList();
            // 全部审批人均与提交人一致，当前节点自动通过：生成留痕记录并继续向后续节点生成记录（issue #226）
            if (operators.isEmpty()) {
                return this.autoPassAndGenerateNextNodeRecords(session, records);
            }
        }
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
     * 当前节点自动通过（审批人均为提交人本人且配置相同人员自动审批，issue #226）。
     *
     * <p>自动通过不等于静默跳过：为当前节点生成一条无审批动作的已办记录
     * （{@link FlowRecord#autoDone()}，与或签/并签遗留待办的自动办结同一语义，
     * 展示层依据 {@link FlowRecord#isAutoDone()} 标记 autoSkip），并立即持久化、
     * 推送已办事件。该记录随后作为记录链上的前驱，继续向后续节点生成记录，
     * 保证流程记录与节点展示中保留当前节点的审批痕迹。
     *
     * <p>记录不入调用方返回列表：调用方（如 {@code PassAction}）在后续节点触发完成后
     * 才统一保存返回列表，若本记录走同一保存路径，会以运行中状态覆盖结束节点
     * {@code fillNewRecord} 已写入的流程结束状态。
     *
     * <p>已办事件在本节点下游记录流转成功之后推送：下游生成（取审批人、异常跳转等）是
     * 最易抛出异常的环节，先流转后推事件保证抛错时事件尚未派发；推送时刻该记录已被
     * 结束节点 {@code over()} 定型，订阅方拿到的载荷即终态。事件相对调用方
     * {@code FlowRecordDoneEvent(前驱)} 的先后次序不受控，与
     * {@code EndNode#fillNewRecord} 直推 {@code FlowRecordFinishEvent} 属同一引擎既有模式。
     *
     * @param session 当前会话（currentNode 为自动通过的节点）
     * @param records 当前调用已累计的记录集合
     * @return 本记录不入列，返回后续节点生成的流程记录，可能为空
     */
    private List<FlowRecord> autoPassAndGenerateNextNodeRecords(FlowSession session, List<FlowRecord> records) {
        FlowRecord currentRecord = session.getCurrentRecord();
        if (currentRecord != null) {
            // 被动式环检测：与抄送节点（NotifyNode）同一模式，时间窗口内同一流程实例的
            // 同一节点再次自动通过即判定为自动流转环，在任何留痕落库/事件派发之前终止
            String traceKey = currentRecord.getProcessId() + ":AUTO_PASS:" + this.getId();
            if (LoopTriggerTraceContext.getInstance().trace(traceKey)) {
                throw FlowExecutionException.nodeLoopDepthExceeded(session.getWorkflow().getMaxNestDepth());
            }
        }
        FlowRecord autoPassRecord = new FlowRecord(session.updateSession(session.getCurrentOperator()), 0);
        autoPassRecord.cleanAction();
        autoPassRecord.autoDone();
        // 先行持久化：级联直达结束节点时，EndNode.fillNewRecord 按 processId 加载历史并标记完成，
        // 必须包含本记录，否则本记录以运行中状态滞留、流程终态不一致
        session.getRepositoryHolder().saveRecord(autoPassRecord);
        // 本记录作为后续节点记录的前驱，fromId 链经过当前节点
        session.setCurrentRecord(autoPassRecord);
        records.addAll(this.generateNextNodeRecords(session));
        EventPusher.push(new FlowRecordDoneEvent(autoPassRecord, session.isMock()));
        return records;
    }

    /**
     * 当前节点自动通过（如审批人均为提交人本人且配置相同人员自动审批），
     * 继续向后续节点生成流程记录。
     *
     * <p>与 {@link com.codingapi.flow.action.BaseAction#triggerNode} 的节点遍历语义一致：
     * 控制节点（条件/并行等）递归深入，业务节点直接生成记录（issue #224）。
     *
     * @param session 当前会话（currentNode 为自动通过的节点）
     * @return 后续节点生成的流程记录，可能为空
     */
    private List<FlowRecord> generateNextNodeRecords(FlowSession session) {
        List<IFlowNode> nextNodes = session.matchNextNodes();
        if (nextNodes == null || nextNodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<FlowRecord> records = new ArrayList<>();
        for (IFlowNode node : nextNodes) {
            FlowSession nextSession = session.updateSession(node);
            if (node.handle(nextSession)) {
                records.addAll(this.generateNextNodeRecords(nextSession));
            } else {
                records.addAll(node.generateCurrentRecords(nextSession));
            }
        }
        return records;
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
