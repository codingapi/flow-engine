package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
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
 * 转办
 */
public class TransferAction extends BaseAction {

    /**
     * 可以转办的人员范围
     */
    private OperatorLoadScript script;

    public TransferAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "转办";
        this.type = ActionType.TRANSFER.name();
        this.display = new ActionDisplay(this.title);
        // 默认 anyone
        this.script = null;
    }

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


    @Override
    public void run(FlowSession flowSession) {
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();

        FlowRecord currentRecord = flowSession.getCurrentRecord();

        currentRecord.update(flowSession, true);
        recordList.add(currentRecord);
        flowEvents.add(new FlowRecordDoneEvent(currentRecord));

        List<IFlowOperator> operators = flowSession.getAdvice().getForwardOperators();
        if (script != null) {
            OperatorManager operatorManager = new OperatorManager(script.execute(flowSession));
            for (IFlowOperator auditOperator : operators) {
                if (!operatorManager.match(auditOperator)) {
                    throw FlowExecutionException.operatorNotInScope("transfer");
                }
            }
        }

        for (IFlowOperator operator : operators) {
            FlowRecord flowRecord = currentRecord.create(flowSession.updateSession(operator));
            recordList.add(flowRecord);
            flowEvents.add(new FlowRecordTodoEvent(flowRecord));
        }

        RepositoryHolderContext.getInstance().saveRecords(recordList);
        flowEvents.forEach(EventPusher::push);
    }

    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((TransferAction) action).script;
    }


    public static TransferAction fromMap(Map<String, Object> data) {
        TransferAction action = BaseAction.fromMap(data, TransferAction.class);
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
