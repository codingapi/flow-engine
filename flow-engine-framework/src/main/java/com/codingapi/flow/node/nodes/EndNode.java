package com.codingapi.flow.node.nodes;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.PassAction;
import com.codingapi.flow.builder.BaseNodeBuilder;
import com.codingapi.flow.event.FlowRecordFinishEvent;
import com.codingapi.flow.node.BaseFlowNode;
import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.NodeType;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.session.FlowSession;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.utils.RandomUtils;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 结束节点
 */
public class EndNode extends BaseFlowNode implements IDisplayNode {

    public static final String NODE_TYPE = NodeType.END.name();
    public static final String DEFAULT_NAME = "结束节点";

    private final ScheduledExecutorService threadPools = Executors.newScheduledThreadPool(1);

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
        for (FlowRecord historyRecord:historyRecords){
            if(historyRecord.getId()== latestRecord.getId()){
                recordList.add(latestRecord);
            }else {
                recordList.add(historyRecord);
            }
        }

        // 设置状态为完成
        recordList.forEach(item -> {
            item.finish(currentAction instanceof PassAction);
        });
        repositoryHolder.saveRecords(recordList);
        // 流程是否正常结束
        EventPusher.push(new FlowRecordFinishEvent(latestRecord));
    }


    @Override
    public boolean handle(FlowSession session) {
        return false;
    }

    public EndNode(String id, String name) {
        super(id, name);
    }

    public EndNode() {
        this(RandomUtils.generateStringId(), DEFAULT_NAME);
    }

    public static EndNode formMap(Map<String, Object> map) {
        return BaseFlowNode.fromMap(map, EndNode.class);
    }


    private static class RecordMergeHelper {

        private final List<FlowRecord> currentRecords;
        private final List<FlowRecord> historyRecords;

        private final Map<Long,FlowRecord> currentRecordCache;

        private final List<FlowRecord> recordList;

        public RecordMergeHelper(List<FlowRecord> historyRecords, List<FlowRecord> currentRecords) {
            this.historyRecords = historyRecords;
            this.currentRecords = currentRecords;
            this.currentRecordCache = new HashMap<>();
            this.recordList = new ArrayList<>();
            this.initCurrentRecordCache();
        }

        private void initCurrentRecordCache(){
            for (FlowRecord currentRecord:this.currentRecords){
                this.currentRecordCache.put(currentRecord.getId(),currentRecord);
            }
        }

        public List<FlowRecord> getUpdateRecordList() {
            for(FlowRecord historyRecord:historyRecords){
                FlowRecord currentRecord =  this.currentRecordCache.get(historyRecord.getId());
                if(currentRecord!=null){
                    this.recordList.add(currentRecord);
                }else {
                    this.recordList.add(historyRecord);
                }
            }
            return this.recordList;
        }



    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseNodeBuilder<Builder, EndNode> {
        public Builder() {
            super(new EndNode());
        }
    }
}
