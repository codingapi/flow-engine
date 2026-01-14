package com.codingapi.flow.service.impl;

import com.codingapi.flow.pojo.request.FlowSubmitRequest;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FlowSubmitService {

    private final FlowSubmitRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;


    public void submit(){

    }
}
