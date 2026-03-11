package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowExecutionException;
import com.codingapi.flow.manager.OperatorManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.node.OperatorLoadScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.springboot.framework.event.EventPusher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 委派
 */
public class DelegateAction extends BaseAction {

    /**
     * 可以委派的人员范围
     */
    private OperatorLoadScript script;

    public void setScript(String script) {
        if (StringUtils.hasText(script)) {
            this.script = new OperatorLoadScript(script);
        }
    }

    public DelegateAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "委派";
        this.enable = true;
        this.type = ActionType.DELEGATE.name();
        this.display = new ActionDisplay(this.title);
        // 默认 anyone
        this.script = null;
    }


    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
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
                    throw FlowExecutionException.operatorNotInScope("delegate");
                }
            }
        }

        for (IFlowOperator operator : operators) {
            FlowRecord flowRecord = currentRecord.create(flowSession.updateSession(operator));
            flowRecord.resetDelegate(currentRecord);
            recordList.add(flowRecord);
            flowEvents.add(new FlowRecordTodoEvent(flowRecord));
        }

        repositoryHolder.saveRecords(recordList);
        flowEvents.forEach(EventPusher::push);

    }

    /**
     * 加签的人员范围
     */
    public List<IFlowOperator> operators(FlowSession flowSession) {
        return script.execute(flowSession);
    }

    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((DelegateAction) action).script;
    }


    public static DelegateAction fromMap(Map<String, Object> data) {
        DelegateAction action = BaseAction.fromMap(data, DelegateAction.class);
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
