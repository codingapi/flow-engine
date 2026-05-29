package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 结束节点
 */
public class EndNode extends BaseFlowNode implements IDisplayNode {

    public static final String NODE_TYPE = NodeType.END.name();
    public static final String DEFAULT_NAME = "结束节点";

    @Override
    public String getType() {
        return NODE_TYPE;
    }


    @Override
    public List<FlowRecord> generateCurrentRecords(FlowSession session) {
        if (this.isWaitRecordMargeParallelNode(session)) {
            return List.of();
        }
        // 构建结束记录
        FlowRecord finishRecord = new FlowRecord(session, 0);
        finishRecord.finish(true);
        return List.of(finishRecord);
    }

    @Override
    public void fillNewRecord(FlowSession session, FlowRecord flowRecord) {
        IRepositoryHolder repositoryHolder = session.getRepositoryHolder();
        flowRecord.over();
        IFlowAction currentAction = session.getCurrentAction();
        // 标记当前流程结束
        FlowRecord latestRecord = session.getCurrentRecord();

        // 添加历史记录到记录中
        List<FlowRecord> historyRecords = repositoryHolder.findProcessRecords(latestRecord.getProcessId());

        // 同步当前处理的FlowRecord的数据
        List<FlowRecord> recordList = new ArrayList<>();
        for (FlowRecord historyRecord : historyRecords) {
            if (historyRecord.getId() == latestRecord.getId()) {
                recordList.add(latestRecord);
            } else {
                recordList.add(historyRecord);
            }
        }

        // 设置状态为完成
        recordList.forEach(item -> {
            item.finish(currentAction instanceof PassAction);
        });
        repositoryHolder.saveRecords(recordList);
        // 流程是否正常结束
        EventPusher.push(new FlowRecordFinishEvent(latestRecord, session.isMock()));
    }


    @Override
    public boolean handle(FlowSession session) {
        return false;
    }


    public static EndNode defaultNode() {
        EndNode endNode = new EndNode();
        endNode.setId(FlowIDGeneratorGatewayContext.getInstance().generateNodeId());
        endNode.setName(DEFAULT_NAME);
        endNode.setActions(new ArrayList<>());
        endNode.setStrategies(new ArrayList<>());
        return endNode;
    }


    public static EndNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, EndNode.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, EndNode> {
        public Builder() {
            super(EndNode.defaultNode());
        }
    }
}
