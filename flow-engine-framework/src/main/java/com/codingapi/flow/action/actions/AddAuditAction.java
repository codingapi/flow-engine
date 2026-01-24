package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.springboot.framework.event.EventPusher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 加签
 */
public class AddAuditAction extends BaseAction {

    /**
     * 可以加签的人员范围
     */
    private OperatorLoadScript script;

    public void setScript(String script) {
        if (StringUtils.hasText(script)) {
            this.script = new OperatorLoadScript(script);
        }
    }

    /**
     * 加签的人员范围
     */
    public List<IFlowOperator> operators(FlowSession flowSession) {
        return script.execute(flowSession);
    }

    public AddAuditAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "加签";
        this.type = ActionType.ADD_AUDIT.name();
        this.display = new ActionDisplay(this.title);
        // 默认 anyone
        this.script = null;
    }


    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((AddAuditAction) action).script;
    }


    @Override
    public void run(FlowSession flowSession) {
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> flowRecords = new ArrayList<>();
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        IFlowNode currentNode = flowSession.getCurrentNode();
        List<FlowRecord> currentRecords = flowSession.getCurrentNodeRecords();
        List<IFlowOperator> auditOperators = flowSession.getAdvice().getForwardOperators();
        if (script != null) {
            OperatorManager operatorManager = new OperatorManager(script.execute(flowSession));
            for (IFlowOperator auditOperator : auditOperators) {
                if (!operatorManager.match(auditOperator)) {
                    throw FlowExecutionException.operatorNotInScope("addAudit");
                }
            }
        }

        // 要延续流程继续的order序号
        int maxNodeOrder = 0;
        for (FlowRecord record : currentRecords) {
            if (record.getNodeOrder() >= maxNodeOrder) {
                maxNodeOrder = record.getNodeOrder();
            }
        }

        long fromId = currentRecord.getFromId();
        // 构建加签的记录
        for (IFlowOperator operator : auditOperators) {
            List<FlowRecord> records = currentNode.generateCurrentRecords(flowSession.updateSession(operator));
            for (FlowRecord record : records) {
                NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
                if (nodeStrategyManager.isSequenceMultiOperatorType()) {
                    record.resetAddAudit(fromId, ++maxNodeOrder, operator.getUserId(), true);
                } else {
                    record.resetAddAudit(fromId, ++maxNodeOrder, operator.getUserId(), false);
                }
            }
            flowRecords.addAll(records);
        }

        for (FlowRecord record : flowRecords) {
            if (record.isShow()) {
                flowEvents.add(new FlowRecordTodoEvent(record));
            }
        }

        RepositoryHolderContext.getInstance().saveRecords(flowRecords);
        flowEvents.forEach(EventPusher::push);
    }

    public static AddAuditAction fromMap(Map<String, Object> data) {
        AddAuditAction action = BaseAction.fromMap(data, AddAuditAction.class);
        String script = (String) data.get("script");
        action.setScript(script);
        return action;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> data = super.toMap();
        if (script != null) {
            data.put("script", script.getScript());
        }
        return data;
    }
}
