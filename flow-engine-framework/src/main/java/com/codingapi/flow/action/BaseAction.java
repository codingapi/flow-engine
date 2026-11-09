package com.codingapi.flow.action;

import com.codingapi.flow.common.IMapConvertor;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@Setter
public abstract class BaseAction implements IFlowAction {

    protected String id;
    protected String type;
    protected String title;
    protected boolean enable;
    protected ActionDisplay display;

    @Override
    public String type() {
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

    @Override
    public boolean show(FlowSession flowSession) {
        return this.display.show(flowSession);
    }

    @Override
    public boolean enable() {
        return enable;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseAction action) {
            return action.getId().equals(id);
        }
        return super.equals(obj);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("title", title);
        map.put("enable", enable);
        map.put("display", display != null ? display.toMap() : null);
        return map;
    }

    @Override
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
        return List.of();
    }

    @Override
    public void copy(IFlowAction action) {
        this.id = action.id();
        this.type = action.type();
        this.title = action.title();
        this.display = action.display();
        this.enable = action.enable();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    protected static <T extends BaseAction> T fromMap(Map<String, Object> data, Class<T> clazz) {
        T action = IMapConvertor.fromMap(data, clazz);
        action.setId((String) data.get("id"));
        action.setType((String) data.get("type"));
        action.setTitle((String) data.get("title"));
        if(data.get("enable")!=null) {
            action.setEnable((boolean) data.get("enable"));
        }else {
            action.setEnable(false);
        }
        Map<String, Object> display = (Map<String, Object>) data.get("display");
        if (display != null) {
            action.setDisplay(ActionDisplay.fromMap(display));
        }
        return action;
    }


    @Override
    public void run(FlowSession flowSession) {
    }

    /**
     * 是否为结束节点虚拟记录的待办事件（非真实业务待办，推送时需要拦截）
     */
    protected boolean isEndNodeTodoEvent(IFlowEvent event) {
        return event instanceof FlowRecordTodoEvent todoEvent
                && !todoEvent.getFlowRecord().isNotEndNode();
    }

    /**
     * 将当前节点实例中其他未办理的待办记录自动办结。
     *
     * <p>退回（{@link com.codingapi.flow.action.actions.ReturnAction}）、拒绝
     * （{@link com.codingapi.flow.action.actions.RejectAction}）意味着当前节点就此结束，
     * 同节点其他审批人（或签/并签的候选审批人）的待办不能继续办理：否则会遗留待办，
     * 流程视图仍停留在当前节点，发起人重新提交后还会出现同流程的重复待办（issue #235 / #255）。
     *
     * <p>自动办结采用 {@link FlowRecord#autoDone()}：记录置为已办但不带审批动作，
     * 展示层标记为「自动跳过」，与或签/并签节点完成后遗留待办的处理口径一致。
     * 仅处理同一流程实例内的记录，避免仓储按 {@code (fromId, nodeId)} 查询时误伤其他流程实例。
     *
     * @param flowSession 当前会话，同节点记录取自 {@link FlowSession#getCurrentNodeRecords()}
     * @param recordList  待保存的记录集合，自动办结的记录会追加其中
     * @param flowEvents  待推送的事件集合，每条自动办结记录都会追加一条已办事件
     */
    protected void autoDoneSiblingTodos(FlowSession flowSession,
                                       List<FlowRecord> recordList,
                                       List<IFlowEvent> flowEvents) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        for (FlowRecord record : flowSession.getCurrentNodeRecords()) {
            if (record.getId() == currentRecord.getId()) {
                continue;
            }
            if (!record.isTodo()) {
                continue;
            }
            if (!currentRecord.getProcessId().equals(record.getProcessId())) {
                continue;
            }
            record.autoDone();
            recordList.add(record);
            flowEvents.add(new FlowRecordDoneEvent(record, flowSession.isMock()));
        }
    }

    /**
     * 触发并执行后续节点
     *
     * @param flowSession 当前会话
     * @param consumer    节点处理
     */
    public void triggerNode(FlowSession flowSession, Consumer<FlowSession> consumer) {
        List<IFlowNode> nextNodes = flowSession.matchNextNodes();
        for (IFlowNode node : nextNodes) {
            FlowSession triggerSession = flowSession.updateSession(node);
            if (node.handle(triggerSession)) {
                this.triggerNode(triggerSession, consumer);
            } else {
                if (consumer != null) {
                    consumer.accept(triggerSession);
                }
            }
        }
    }
}
