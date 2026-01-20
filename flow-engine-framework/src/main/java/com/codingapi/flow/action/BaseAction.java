package com.codingapi.flow.action;

import com.codingapi.flow.context.RepositoryContext;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.nodes.EndNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.springboot.framework.event.EventPusher;
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
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
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
        Map<String, Object> display = (Map<String, Object>) data.get("display");
        if (display != null) {
            action.setDisplay(ActionDisplay.fromMap(display));
        }
        return action;
    }


    @Override
    public void triggerNode(FlowSession flowSession) {
        List<IFlowNode> nextNodes = flowSession.nextNodes();
        for (IFlowNode node : nextNodes) {
            FlowSession triggerSession = flowSession.updateSession(node);
            if (node.continueTrigger(triggerSession)) {
                this.triggerNode(triggerSession);
            }
        }
    }


    public void flowFinish(FlowRecord latestRecord,IFlowNode latestNode){
        List<FlowRecord> recordList = new ArrayList<>();
        // 添加当前节点到记录中
        if (latestNode instanceof EndNode) {
            recordList.add(latestRecord);
            // 添加历史记录到记录中
            List<FlowRecord> historyRecords = RepositoryContext.getInstance().findRecordsByProcessId(latestRecord.getProcessId());
            recordList.addAll(historyRecords);
            // 设置状态为完成
            recordList.forEach(item -> {
                item.finish(true);
            });

            RepositoryContext.getInstance().saveRecords(recordList);

            // 流程是否正常结束
            EventPusher.push(new FlowRecordFinishEvent(latestRecord));
        }
    }

}
