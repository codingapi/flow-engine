package com.codingapi.flow.service;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.impl.FlowActionService;
import com.codingapi.flow.service.impl.FlowCreateService;
import org.springframework.context.annotation.Configuration;

/**
 * 流程服务
 */
@Configuration
public class FlowService {

    private final WorkflowRepository workflowRepository;
    private final FlowOperatorGateway flowOperatorGateway;
    private final FlowRecordRepository flowRecordRepository;
    private final WorkflowBackupRepository workflowBackupRepository;

    public FlowService(WorkflowRepository workflowRepository,
                       FlowOperatorGateway flowOperatorGateway,
                       FlowRecordRepository flowRecordRepository,
                       WorkflowBackupRepository workflowBackupRepository,
                       ParallelBranchRepository parallelBranchRepository,
                       DelayTaskRepository delayTaskRepository) {
        this.workflowRepository = workflowRepository;
        this.flowOperatorGateway = flowOperatorGateway;
        this.flowRecordRepository = flowRecordRepository;
        this.workflowBackupRepository = workflowBackupRepository;

        RepositoryHolderContext.getInstance()
                .register(workflowRepository,
                        workflowBackupRepository,
                        flowRecordRepository,
                        flowOperatorGateway,
                        parallelBranchRepository,
                        delayTaskRepository);
    }

    /**
     * 创建流程
     * @param request 创建流程请求
     * @return 创建的流程id
     */
    public long create(FlowCreateRequest request) {
        FlowCreateService flowCreateService = new FlowCreateService(request, flowOperatorGateway, flowRecordRepository, workflowRepository, workflowBackupRepository);
        return flowCreateService.create();
    }

    public void action(FlowActionRequest request) {
        FlowActionService flowActionService = new FlowActionService(request, flowOperatorGateway, flowRecordRepository, workflowBackupRepository);
        flowActionService.action();
    }
}
