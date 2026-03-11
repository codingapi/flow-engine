package com.codingapi.flow.factory;

import com.codingapi.flow.gateway.impl.UserGateway;
import com.codingapi.flow.mock.FlowQueryMockService;
import com.codingapi.flow.mock.FlowServiceMockFactory;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.mock.repository.*;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;

public class MyFlowServiceMockFactory {

    public FlowTodoRecordRepositoryMockImpl flowTodoRecordRepository;
    public FlowTodoMergeRepositoryMockImpl flowTodoMergeRepository;
    public FlowRecordRepositoryMockImpl flowRecordRepository;
    public WorkflowRuntimeRepositoryMockImpl workflowRuntimeRepository;
    public WorkflowVersionRepositoryMockImpl workflowVersionRepository;
    public WorkflowRepositoryMockImpl workflowRepository;
    public ParallelBranchRepositoryMockImpl parallelBranchRepository;
    public DelayTaskRepositoryMockImpl delayTaskRepository;
    public UrgeIntervalRepositoryMockImpl urgeIntervalRepository;
    public WorkflowService workflowService;
    public FlowRecordService flowRecordService;
    public FlowService flowService;
    public MockRepositoryHolder repositoryHolder;
    public UserGateway userGateway;
    public FlowQueryMockService flowQueryMockService;

    public MyFlowServiceMockFactory() {
        userGateway = new UserGateway();
        String key = FlowServiceMockFactory.getInstance().create(userGateway);
        flowService = FlowServiceMockFactory.getInstance().getFlowService(key);
        repositoryHolder = (MockRepositoryHolder)flowService.getRepositoryHolder();
        flowTodoRecordRepository = (FlowTodoRecordRepositoryMockImpl)repositoryHolder.getFlowTodoRecordRepository();
        flowTodoMergeRepository = (FlowTodoMergeRepositoryMockImpl)repositoryHolder.getFlowTodoMergeRepository();
        flowRecordRepository = (FlowRecordRepositoryMockImpl) repositoryHolder.getFlowRecordRepository();
        workflowRuntimeRepository = (WorkflowRuntimeRepositoryMockImpl) repositoryHolder.getWorkflowRuntimeRepository();
        workflowVersionRepository = (WorkflowVersionRepositoryMockImpl)repositoryHolder.getWorkflowVersionRepository();
        workflowRepository = (WorkflowRepositoryMockImpl) repositoryHolder.getWorkflowRepository();
        parallelBranchRepository = (ParallelBranchRepositoryMockImpl) repositoryHolder.getParallelBranchRepository();
        delayTaskRepository = (DelayTaskRepositoryMockImpl) repositoryHolder.getDelayTaskRepository();
        urgeIntervalRepository = (UrgeIntervalRepositoryMockImpl) repositoryHolder.getUrgeIntervalRepository();
        workflowService = repositoryHolder.getWorkflowService();
        flowRecordService = repositoryHolder.getFlowRecordService();
        flowQueryMockService = FlowServiceMockFactory.getInstance().getFlowQueryService(key);
    }
}
