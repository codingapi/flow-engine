package com.codingapi.flow.service.impl;

import com.codingapi.flow.event.FlowRecordUrgeEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.springboot.framework.event.EventPusher;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 *  催办服务
 */
@AllArgsConstructor
public class FlowUrgeService {

    private final FlowUrgeRequest request;
    private final FlowRecordRepository flowRecordRepository;
    private final FlowOperatorGateway flowOperatorGateway;

    /**
     * 催办
     */
    public void urge() {
        request.verify();
        // 验证当前用户
        FlowRecord currentRecord = flowRecordRepository.get(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (currentRecord.isTodo()) {
            throw FlowStateException.recordAlreadyTodo();
        }
        if (currentRecord.isFinish()) {
            throw FlowStateException.recordNotSupportRevoke();
        }
        long currentOperatorId = currentRecord.getCurrentOperatorId();
        if (currentOperatorId != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }
        IFlowOperator currentOperator = flowOperatorGateway.get(currentOperatorId);

        List<FlowRecord> todoRecords = flowRecordRepository.findTodoRecords(currentRecord.getProcessId());

        List<IFlowEvent> flowEvents = new ArrayList<>();

        for (FlowRecord todoRecord : todoRecords) {
            flowEvents.add(new FlowRecordUrgeEvent(todoRecord,currentOperator));
        }

        flowEvents.forEach(EventPusher::push);
    }
}
