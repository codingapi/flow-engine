package com.codingapi.flow.action;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.NodeOperators;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.RejectActionScript;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
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

    public static RejectAction formMap(Map<String, Object> data) {
        RejectAction rejectAction = BaseAction.formMap(data, RejectAction.class);
        String script = (String) data.get("script");
        rejectAction.setScript(script);
        return rejectAction;
    }

    @Override
    public List<FlowRecord> trigger(FlowSession flowSession, FlowRecord currentRecord) {
        RejectActionScript.RejectResult rejectResult = script.execute(flowSession);
        IFlowNode currentNode = null;
        if (rejectResult.isReturnNode()) {
            String nodeId = rejectResult.getNodeId();
            currentNode = flowSession.getWorkflow().getNode(nodeId);
        }
        if (rejectResult.isTerminate()) {
            currentNode = flowSession.getWorkflow().getEndNode();
        }
        if (rejectResult.isReturnPrev()) {
            long fromId = currentRecord.getFromId();
            FlowRecord preRecord = FlowScriptContext.getInstance().getRecordById(fromId);
            if (preRecord == null) {
                throw new IllegalArgumentException("preRecord is null");
            }
            currentNode = flowSession.getWorkflow().getNode(preRecord.getNodeId());
        }
        if (currentNode == null) {
            throw new IllegalArgumentException("currentNode is null");
        }
        List<FlowRecord> records = new ArrayList<>();
        FlowSession triggerSession = flowSession.updateSession(currentNode);
        NodeOperators nodeOperators = currentNode.operators(triggerSession);
        for (IFlowOperator operator : nodeOperators.getOperators()) {
            FlowRecord flowRecord = new FlowRecord(triggerSession.updateSession(operator), this.id, currentRecord.getProcessId(), currentRecord.getId());
            flowRecord.setReturnNodeId(currentRecord.getNodeId());
            records.add(flowRecord);
        }
        return records;
    }
}
