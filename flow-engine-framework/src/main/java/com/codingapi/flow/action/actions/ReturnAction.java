package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
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

        // 退回目标为抄送/子流程节点时无法产生有效记录，直接拒绝
        BaseFlowNode.verifyJumpTarget(backNode);

        FlowRecord currentRecord = flowSession.getCurrentRecord();
        currentRecord.update(flowSession, true);
        recordList.add(currentRecord);

        flowEvents.add(new FlowRecordDoneEvent(currentRecord,flowSession.isMock()));

        // 退回即结束当前节点：同节点其他未办理的待办（或签/并签的其他审批人）一并自动置为已办。
        // 否则退回后仍会遗留待办，发起人重新提交到该节点时会出现同流程的重复待办（issue #255）。
        this.autoDoneSiblingTodos(flowSession, recordList, flowEvents);

        // 先保存当前记录与同节点待办的自动办结状态，再生成退回目标节点的记录，
        // 与拒绝动作一致，避免退回目标节点的生成过程覆盖本会话中的记录状态（issue #235）
        repositoryHolder.saveRecords(recordList);
        recordList.clear();

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
