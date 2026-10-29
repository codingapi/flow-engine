package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.action.ActionRejectScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拒绝动作
 * 拒绝，拒绝时需要根据拒绝的配置流程来设置,退回上级节点、退回指定节点、终止流程
 */
public class RejectAction extends BaseAction {

    public static final String DEFAULT_TITLE = "拒绝";

    @Getter
    private ActionRejectScript script;


    public static RejectAction defaultAction() {
        RejectAction action = new RejectAction();
        action.setId(FlowIDGeneratorGatewayContext.getInstance().generateActionId());
        action.setTitle(DEFAULT_TITLE);
        action.setEnable(true);
        action.setType(ActionType.REJECT.name());
        action.setDisplay(ActionDisplay.defaultDisplay(DEFAULT_TITLE));
        action.script =  ActionRejectScript.defaultScript();;
        return action;
    }


    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((RejectAction) action).script;
    }

    public void setScript(String script) {
        this.script = new ActionRejectScript(script);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("script", script.getScript());
        return map;
    }

    public static RejectAction fromMap(Map<String, Object> data) {
        RejectAction rejectAction = BaseAction.fromMap(data, RejectAction.class);
        String script = (String) data.get("script");
        rejectAction.setScript(script);
        return rejectAction;
    }

    @Override
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
        ActionRejectScript.RejectResult rejectResult = script.execute(flowSession);
        IFlowNode currentNode = null;
        // 返回指定节点
        if (rejectResult.isReturnNode()) {
            String nodeId = rejectResult.getNodeId();
            currentNode = flowSession.getWorkflow().getFlowNode(nodeId);
        }
        // 流程结束（非正常）
        if (rejectResult.isTerminate()) {
            currentNode = flowSession.getWorkflow().getEndNode();
        }
        if (currentNode == null) {
            throw FlowStateException.currentNodeNotNull();
        }
        // 退回目标为抄送/子流程节点时无法产生有效记录，直接拒绝
        BaseFlowNode.verifyJumpTarget(currentNode);
        flowSession = flowSession.updateSession(currentNode);
        return currentNode.generateCurrentRecords(flowSession);
    }

    @Override
    public void run(FlowSession flowSession) {
        IRepositoryHolder repositoryHolder = flowSession.getRepositoryHolder();
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();

        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.update(flowSession, true);
        recordList.add(flowRecord);

        // 当前审批为已办状态
        flowEvents.add(new FlowRecordDoneEvent(flowRecord, flowSession.isMock()));

        // 拒绝即结束当前节点：将同节点其他未办理的待办（或签/并签的其他审批人）自动置为已办。
        // 否则节点被退回后仍会遗留待办，审批人可继续办理，流程视图也会停留在当前节点（issue #235）。
        for (FlowRecord record : flowSession.getCurrentNodeRecords()) {
            if (record.getId() != flowRecord.getId() && record.isTodo()) {
                record.autoDone();
                recordList.add(record);
                flowEvents.add(new FlowRecordDoneEvent(record, flowSession.isMock()));
            }
        }

        // 先保存当前记录及同节点待办的自动办结状态，再触发退回/终止节点。
        // 终止流程时 EndNode.fillNewRecord 会按数据库中的最新记录统一置为结束状态，
        // 若在触发后再统一保存，会用本会话中陈旧的“运行中”状态覆盖终态（issue #235 / #184）。
        repositoryHolder.saveRecords(recordList);
        recordList.clear();

        List<FlowRecord> records = this.generateRecords(flowSession);
        if (!records.isEmpty()) {
            for (FlowRecord record : records) {
                if (record.isShow()) {
                    flowEvents.add(new FlowRecordTodoEvent(record, flowSession.isMock()));
                }
            }
            recordList.addAll(records);
        }
        repositoryHolder.saveRecords(recordList);

        // 结束节点的虚拟记录不是真实业务待办，其 Todo 事件推送业务订阅方前需拦截
        flowEvents.stream()
                .filter(event -> !isEndNodeTodoEvent(event))
                .forEach(EventPusher::push);

    }
}
