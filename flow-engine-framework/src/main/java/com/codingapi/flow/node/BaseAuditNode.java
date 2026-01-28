package com.codingapi.flow.node;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.error.ErrorThrow;
import com.codingapi.flow.exception.FlowConfigException;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.node.INodeStrategy;
import com.codingapi.flow.strategy.node.MultiOperatorAuditStrategy;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseAuditNode extends BaseFlowNode implements IFlowNode {

    public static final String DEFAULT_VIEW = "default";

    /**
     * 渲染视图
     */
    @Getter
    @Setter
    private String view;


    public BaseAuditNode(String id, String name, String view, List<IFlowAction> actions, List<INodeStrategy> nodeStrategies) {
        super(id, name, 0, actions, nodeStrategies);
        this.view = view;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("view", view);
        return map;
    }


    public void verifyNode(FormMeta form) {
        super.verifyNode(form);
        if (!StringUtils.hasText(view)) {
            throw FlowConfigException.viewNotNull();
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
        flowRecord.setMergeable(nodeStrategyManager.isMergeable());
        flowRecord.newRecord();
    }

    @Override
    public boolean isDone(FlowSession session) {
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
                throw FlowConfigException.errorThrowNotNull();
            }
            if(errorThrow.isNode()){
                IFlowNode errorNode = errorThrow.getNode();
                FlowSession errorSession = session.updateSession(errorNode);
                return errorNode.generateCurrentRecords(errorSession);
            }else {
                operatorManager = new OperatorManager(errorThrow.getOperators());
            }
        }
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(session.updateSession(operator), order);
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
                int index = RandomUtils.randomInt(operators.size());

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


    @SneakyThrows
    public static <T extends BaseAuditNode> T formMap(Map<String, Object> map, Class<T> clazz) {
        T node = BaseFlowNode.fromMap(map, clazz);
        node.setView((String) map.get("view"));
        return node;
    }

}
