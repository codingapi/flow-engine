package com.codingapi.flow.factory;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;

public class MyFlowServiceFactory {

    public FlowTodoRecordRepositoryImpl flowTodoRecordRepository;
    public FlowTodoMergeRepositoryImpl flowTodoMergeRepository;
    public FlowRecordRepositoryImpl flowRecordRepository;
    public UserGateway userGateway;
    public WorkflowRuntimeRepository workflowRuntimeRepository;
    public WorkflowVersionRepository workflowVersionRepository;
    public WorkflowRepository workflowRepository;
    public ParallelBranchRepository parallelBranchRepository;
    public DelayTaskRepository delayTaskRepository;
    public UrgeIntervalRepository urgeIntervalRepository;
    public WorkflowService workflowService;
    public FlowRecordService flowRecordService;
    public FlowService flowService;
    public IRepositoryHolder repositoryHolder;

    public MyFlowServiceFactory() {
        flowTodoRecordRepository = new FlowTodoRecordRepositoryImpl();
        flowTodoMergeRepository = new FlowTodoMergeRepositoryImpl();
        flowRecordRepository = new FlowRecordRepositoryImpl();
        userGateway = new UserGateway();
        workflowRuntimeRepository = new WorkflowRuntimeRepositoryImpl();
        workflowVersionRepository = new WorkflowVersionRepositoryImpl();
        workflowRepository = new WorkflowRepositoryImpl();
        parallelBranchRepository = new ParallelBranchRepositoryImpl();
        delayTaskRepository = new DelayTaskRepositoryImpl();
        urgeIntervalRepository = new UrgeIntervalRepositoryImpl();
        workflowService = new WorkflowService(workflowVersionRepository, workflowRepository, workflowRuntimeRepository);
        flowRecordService = new FlowRecordService(flowTodoRecordRepository, flowTodoMergeRepository, flowRecordRepository);

        RepositoryHolderContext.getInstance().register(workflowService, flowRecordService, userGateway, parallelBranchRepository, delayTaskRepository, urgeIntervalRepository);
        repositoryHolder = RepositoryHolderContext.getInstance();
        this.flowService = new FlowService(this.repositoryHolder);

        GatewayContext.getInstance().setFlowOperatorGateway(userGateway);
    }

}
