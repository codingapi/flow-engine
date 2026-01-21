package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.action.RejectActionScript;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
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

    @Getter
    private RejectActionScript script;

    public RejectAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "拒绝";
        this.type = ActionType.REJECT;
        this.display = new ActionDisplay(this.title);
        this.script = RejectActionScript.startScript();
    }

    @Override
    public void copy(IFlowAction action) {
        super.copy(action);
        this.script = ((RejectAction) action).script;
    }

    public void setScript(String script) {
        this.script = new RejectActionScript(script);
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
        RejectActionScript.RejectResult rejectResult = script.execute(flowSession);
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
            throw new IllegalArgumentException("currentNode is null");
        }
        flowSession = flowSession.updateSession(currentNode);
        return currentNode.generateCurrentRecords(flowSession);
    }

    @Override
    public void run(FlowSession flowSession) {
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();

        FlowRecord flowRecord = flowSession.getCurrentRecord();
        flowRecord.update(flowSession.getFormData().toMapData(), flowSession.getAdvice().getAdvice(), flowSession.getAdvice().getSignKey(), true);
        recordList.add(flowRecord);

        List<FlowRecord> records = this.generateRecords(flowSession);
        if(!records.isEmpty()) {
            recordList.addAll(records);
            for (FlowRecord record : records) {
                if (record.isShow()) {
                    flowEvents.add(new FlowRecordTodoEvent(record));
                }
            }
        }
        RepositoryContext.getInstance().saveRecords(recordList);
        flowEvents.forEach(EventPusher::push);

    }
}
