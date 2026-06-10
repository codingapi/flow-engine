package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 退回
 */
public class ReturnAction extends BaseAction {

    public static final String DEFAULT_TITLE = "退回";


    public static ReturnAction defaultAction() {
        ReturnAction action = new ReturnAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.RETURN.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        return action;
    }

    public static ReturnAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, ReturnAction.class);
    }


    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();

        IFlowNode backNode = flowSession.getAdvice().getBackNode();

        FlowRecord currentRecord = flowSession.getCurrentRecord();
        currentRecord.update(flowSession, true);
        recordList.add(currentRecord);

        flowEvents.add(new FlowRecordDoneEvent(currentRecord,flowSession.isMock()));

        List<FlowRecord> flowRecords = backNode.generateCurrentRecords(flowSession.updateSession(backNode));
        recordList.addAll(flowRecords);

        for (FlowRecord record : flowRecords) {
            if (record.isShow()) {
                flowEvents.add(new FlowRecordTodoEvent(record,flowSession.isMock()));
            }
        }

        repositoryHolder.saveRecords(recordList);

        flowEvents.forEach(EventPusher::push);

    }
}
