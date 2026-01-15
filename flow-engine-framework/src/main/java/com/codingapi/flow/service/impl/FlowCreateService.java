package com.codingapi.flow.service.impl;

import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.repository.WorkflowBackupRepository;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.workflow.Workflow;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FlowCreateService {

    private final FlowCreateRequest request;
    private final FlowOperatorGateway flowOperatorGateway;
    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public void create() {
        Workflow workflow = workflowRepository.get(request.getWorkId());
        if(workflow==null){
            throw new IllegalArgumentException("workflow not found");
        }
        workflow.verify();
    }
}
