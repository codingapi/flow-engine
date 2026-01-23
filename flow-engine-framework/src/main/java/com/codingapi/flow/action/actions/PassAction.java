package com.codingapi.flow.action.actions;

import com.codingapi.flow.action.ActionDisplay;
import com.codingapi.flow.action.ActionType;
import com.codingapi.flow.action.BaseAction;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.event.FlowRecordDoneEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.node.BaseAuditNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.manager.StrategyManager;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 通过
 */
public class PassAction extends BaseAction {

    public PassAction() {
        this.id = RandomUtils.generateStringId();
        this.title = "通过";
        this.type = ActionType.PASS.name();
        this.display = new ActionDisplay(this.title);
    }

    public static PassAction fromMap(Map<String, Object> data) {
        return BaseAction.fromMap(data, PassAction.class);
    }


    @Override
    public List<FlowRecord> generateRecords(FlowSession flowSession) {
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        List<FlowRecord> records = new ArrayList<>();
        if (currentRecord.isReturnRecord()) {
            // 退回后的流程重新提交
            BaseAuditNode currentNode = (BaseAuditNode) flowSession.getWorkflow().getFlowNode(currentRecord.getReturnNodeId());
            StrategyManager strategyManager = currentNode.strategyManager();
            // 是否退回到退回节点
            if (strategyManager.isResume()) {
                FlowSession triggerSession = flowSession.updateSession(currentNode);
                List<FlowRecord> nextRecords = currentNode.generateCurrentRecords(triggerSession.updateSession(currentNode));
                records.addAll(nextRecords);
            }
        } else {
            IFlowNode currentNode = flowSession.getCurrentNode();
            List<FlowRecord> nextRecords = currentNode.generateCurrentRecords(flowSession);
            if (!nextRecords.isEmpty()) {
                records.addAll(nextRecords);
            }
        }
        return records;
    }


    @Override
    public void run(FlowSession flowSession) {
        List<IFlowEvent> flowEvents = new ArrayList<>();
        List<FlowRecord> recordList = new ArrayList<>();
        FlowRecord currentRecord = flowSession.getCurrentRecord();
        IFlowNode currentNode = flowSession.getCurrentNode();
        boolean done = currentNode.isDone(flowSession);
        currentRecord.update(flowSession, done);
        // 添加流程结束事件
        flowEvents.add(new FlowRecordDoneEvent(currentRecord));
        recordList.add(currentRecord);

        // 激活下一个按顺序审批的记录数据
        StrategyManager strategyManager = currentNode.strategyManager();
        if (strategyManager.isSequenceMultiOperatorType()) {
            List<FlowRecord> currentRecords = flowSession.getCurrentNodeRecords();
            for (FlowRecord record : currentRecords) {
                if (record.getNodeOrder() == currentRecord.getNodeOrder() + 1) {
                    record.show();
                    recordList.add(record);
                    flowEvents.add(new FlowRecordTodoEvent(record));
                }
            }
        }

        if (done) {
            // 是否委托记录
            if (currentRecord.isDelegate()) {
                FlowRecord delegateRecord = RepositoryHolderContext.getInstance().getRecordById(currentRecord.getDelegateId());
                IFlowOperator delegateOperator = RepositoryHolderContext.getInstance().getOperatorById(delegateRecord.getCurrentOperatorId());
                FlowRecord rebackRecord = delegateRecord.copy(flowSession.updateSession(delegateOperator));
                rebackRecord.clearDelegate();

                recordList.add(rebackRecord);
                flowEvents.add(new FlowRecordTodoEvent(rebackRecord));
            } else {
                this.triggerNode(flowSession, (triggerSession) -> {
                    List<FlowRecord> records = this.generateRecords(triggerSession);
                    if (!records.isEmpty()) {
                        for (FlowRecord record : records) {
                            if (record.isShow()) {
                                flowEvents.add(new FlowRecordTodoEvent(record));
                            }
                        }
                        recordList.addAll(records);
                    }
                });
            }
        }

        RepositoryHolderContext.getInstance().saveRecords(recordList);

        flowEvents.forEach(EventPusher::push);
    }
}
