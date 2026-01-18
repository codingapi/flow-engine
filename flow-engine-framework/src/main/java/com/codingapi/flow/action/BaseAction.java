package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.manager.OperatorManager;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.strategy.MultiOperatorAuditStrategy;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class BaseAction implements IFlowAction {

    protected String id;
    protected ActionType type;
    protected String title;
    protected ActionDisplay display;

    @Override
    public ActionType type() {
        return type;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public ActionDisplay display() {
        return display;
    }


    protected void setId(String id) {
        this.id = id;
    }

    protected void setType(ActionType type) {
        this.type = type;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setDisplay(ActionDisplay display) {
        this.display = display;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("title", title);
        map.put("display", display != null ? display.toMap() : null);
        return map;
    }

    @Override
    public List<FlowRecord> trigger(FlowSession flowSession,FlowRecord currentRecord) {
        return null;
    }

    @Override
    public boolean isDone(FlowSession session, FlowRecord currentRecord, List<FlowRecord> currentRecords) {
        return true;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected static <T extends BaseAction> T fromMap(Map<String, Object> data, Class<T> clazz) {
        T action = clazz.getDeclaredConstructor().newInstance();
        action.setId((String) data.get("id"));
        action.setType(ActionType.valueOf((String) data.get("type")));
        action.setTitle((String) data.get("title"));
        Map<String,Object> display = (Map<String, Object>) data.get("display");
        if(display!=null) {
            action.setDisplay(ActionDisplay.fromMap(display));
        }
        return action;
    }


    /**
     * 生成下一节点的记录
     *
     * @param currentNode    当前节点
     * @param triggerSession 触发会话
     * @param currentRecord  当前记录
     * @return 下一节节点的记录
     */
    protected List<FlowRecord> generateNextRecords(IFlowNode currentNode, FlowSession triggerSession, FlowRecord currentRecord) {
        List<FlowRecord> records = new ArrayList<>();
        OperatorManager operatorManager = currentNode.operators(triggerSession);
        List<IFlowOperator> operators = operatorManager.getOperators();
        for (int order = 0; order < operators.size(); order++) {
            IFlowOperator operator = operators.get(order);
            FlowRecord flowRecord = new FlowRecord(triggerSession.updateSession(operator), this.id, currentRecord.getProcessId(), currentRecord.getId(), order);
            records.add(flowRecord);
        }
        if (operators.size() > 1) {
            StrategyManager strategyManager = currentNode.strategies();
            MultiOperatorAuditStrategy.Type multiOperatorAuditStrategyType = strategyManager.getMultiOperatorAuditStrategyType();
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
}
