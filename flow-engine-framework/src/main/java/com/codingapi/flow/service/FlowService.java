package com.codingapi.flow.service;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowSubmitRequest;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.service.impl.FlowCreateService;
import com.codingapi.flow.service.impl.FlowSubmitService;
import lombok.AllArgsConstructor;

/**
 * 流程服务
 */
@AllArgsConstructor
public class FlowService {

    private final WorkflowRepository workflowRepository;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;


    public void create(FlowCreateRequest request){
        FlowCreateService flowCreateService = new FlowCreateService(request, flowOperatorGateway,flowRecordRepository,workflowRepository,workflowBackupRepository);
        flowCreateService.create();
    }

    public void submit(FlowSubmitRequest request){
        FlowSubmitService flowSubmitService = new FlowSubmitService(request, flowOperatorGateway,flowRecordRepository,workflowBackupRepository);
        flowSubmitService.submit();
    }
}
