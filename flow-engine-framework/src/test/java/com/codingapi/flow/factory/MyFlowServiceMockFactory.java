package com.codingapi.flow.factory;

import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.mock.MockInstance;
import com.codingapi.flow.mock.MockInstanceFactory;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.mock.repository.*;
import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.repository.WorkflowRepositoryImpl;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;

public class MyFlowServiceMockFactory {

    public FlowTodoRecordRepositoryMockImpl flowTodoRecordRepository;
    public FlowTodoMergeRepositoryMockImpl flowTodoMergeRepository;
    public FlowRecordRepositoryMockImpl flowRecordRepository;
    public WorkflowRuntimeRepositoryMockImpl workflowRuntimeRepository;
    public WorkflowVersionRepositoryMockImpl workflowVersionRepository;
    public WorkflowRepositoryImpl workflowRepository;
    public ParallelBranchRepositoryMockImpl parallelBranchRepository;
    public DelayTaskRepositoryMockImpl delayTaskRepository;
    public UrgeIntervalRepositoryMockImpl urgeIntervalRepository;
    public WorkflowService workflowService;
    public FlowRecordService flowRecordService;
    public FlowService flowService;
    public MockRepositoryHolder repositoryHolder;
    public UserGateway userGateway;
    public FlowRecordQueryService flowRecordQueryMockService;

    public MyFlowServiceMockFactory() {
        userGateway = new UserGateway();
        workflowRepository = new WorkflowRepositoryImpl();
        MockInstance mockInstance = MockInstanceFactory.getInstance().create(userGateway,workflowRepository);
        flowService = mockInstance.getFlowService();
        repositoryHolder = (MockRepositoryHolder)flowService.getRepositoryHolder();
        flowTodoRecordRepository = (FlowTodoRecordRepositoryMockImpl)repositoryHolder.getFlowTodoRecordRepository();
        flowTodoMergeRepository = (FlowTodoMergeRepositoryMockImpl)repositoryHolder.getFlowTodoMergeRepository();
        flowRecordRepository = (FlowRecordRepositoryMockImpl) repositoryHolder.getFlowRecordRepository();
        workflowRuntimeRepository = (WorkflowRuntimeRepositoryMockImpl) repositoryHolder.getWorkflowRuntimeRepository();
        workflowVersionRepository = (WorkflowVersionRepositoryMockImpl)repositoryHolder.getWorkflowVersionRepository();
        parallelBranchRepository = (ParallelBranchRepositoryMockImpl) repositoryHolder.getParallelBranchRepository();
        delayTaskRepository = (DelayTaskRepositoryMockImpl) repositoryHolder.getDelayTaskRepository();
        urgeIntervalRepository = (UrgeIntervalRepositoryMockImpl) repositoryHolder.getUrgeIntervalRepository();
        workflowService = repositoryHolder.getWorkflowService();
        flowRecordService = repositoryHolder.getFlowRecordService();
        flowRecordQueryMockService = mockInstance.getFlowRecordQueryService();
    }
}
