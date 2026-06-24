package com.codingapi.flow.service.impl;

import com.codingapi.flow.event.FlowRecordDeleteEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.node.nodes.StartNode;
import com.codingapi.flow.pojo.request.FlowDeleteRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.List;

/**
 * 删除流程服务
 * <p>
 * 仅允许删除位于开始节点、且尚未流转的待办记录,等价于作废整个尚未发起的流程实例。
 * 若流程已流转到后续节点,需先撤销退回开始节点后再删除。
 */
public class FlowDeleteService {

    private final FlowDeleteRequest request;
    private final FlowRecordService flowRecordService;
    private final IRepositoryHolder repositoryHolder;

    public FlowDeleteService(FlowDeleteRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.repositoryHolder = repositoryHolder;
    }

    public void delete() {
        request.verify();
        FlowRecord currentRecord = flowRecordService.getFlowRecord(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        // 必须是待办状态(未办理、未撤销、未结束)
        if (!currentRecord.isTodo()) {
            throw FlowStateException.recordNotSupportDelete();
        }
        // 必须位于开始节点
        if (!currentRecord.isNodeType(StartNode.NODE_TYPE)) {
            throw FlowStateException.nodeNotStartNode();
        }
        // 必须无后续流转记录(流程尚未执行)
        List<FlowRecord> afterRecords = flowRecordService.findFlowRecordAfterRecords(
                currentRecord.getProcessId(), currentRecord.getId());
        if (afterRecords != null && !afterRecords.isEmpty()) {
            throw FlowStateException.recordAlreadyRunning();
        }
        // 权限校验:仅允许当前待办处理人删除
        if (currentRecord.getCurrentOperatorId() != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }
        // 删除流程记录及其关联待办
        flowRecordService.deleteFlowRecord(currentRecord);
        // 推送删除事件
        EventPusher.push(new FlowRecordDeleteEvent(currentRecord, repositoryHolder instanceof MockRepositoryHolder));
    }
}
