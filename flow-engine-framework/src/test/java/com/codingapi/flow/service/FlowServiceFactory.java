package com.codingapi.flow.service;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.session.IRepositoryHolder;

public class FlowServiceFactory {

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

    public FlowServiceFactory() {
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
    }

}
