package com.codingapi.flow.action;

import com.codingapi.flow.node.IAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.action.RejectActionScript;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

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
        this.script = RejectActionScript.defaultScript();
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
    public List<FlowRecord> trigger(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        RejectActionScript.RejectResult rejectResult = script.execute(flowSession);
        IAuditNode currentNode = null;
        // 返回指定节点
        if (rejectResult.isReturnNode()) {
            String nodeId = rejectResult.getNodeId();
            currentNode = flowSession.getWorkflow().getAuditNode(nodeId);
        }
        // 流程结束（非正常）
        if (rejectResult.isTerminate()) {
            currentNode = flowSession.getWorkflow().getEndNode();
        }
        // 退回上级节点
        if (rejectResult.isReturnPrev()) {
            long fromId = currentRecord.getFromId();
            FlowRecord preRecord = FlowScriptContext.getInstance().getRecordById(fromId);
            if (preRecord == null) {
                throw new IllegalArgumentException("preRecord is null");
            }
            currentNode = flowSession.getWorkflow().getAuditNode(preRecord.getNodeId());
        }
        if (currentNode == null) {
            throw new IllegalArgumentException("currentNode is null");
        }

        FlowSession triggerSession = flowSession.updateSession(currentNode);
        return this.generateNextRecords(currentNode, triggerSession, currentRecord);
    }
}
