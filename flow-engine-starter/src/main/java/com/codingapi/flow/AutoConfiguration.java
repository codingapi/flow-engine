package com.codingapi.flow;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.register.RepositoryHolderContextRegister;
import com.codingapi.flow.register.FlowScriptContextRegister;
import com.codingapi.flow.register.GatewayContextRegister;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.runner.FlowDelayTaskRunner;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfiguration {

    @Bean
    public FlowDelayTaskRunner delayTaskRunner() {
        return new FlowDelayTaskRunner();
    }

    @Bean
    public GatewayContextRegister gatewayContextRegister(FlowOperatorGateway flowOperatorGateway) {
        return new GatewayContextRegister(flowOperatorGateway);
    }

    @Bean
    public FlowScriptContextRegister flowScriptContextRegister(
            ApplicationContext spring,
            FlowOperatorGateway flowOperatorGateway,
            FlowRecordRepository flowRecordRepository
            ) {
        return new FlowScriptContextRegister(spring, flowOperatorGateway,flowRecordRepository);
    }

    @Bean
    public RepositoryHolderContextRegister repositoryHolderContextRegister(
            WorkflowService workflowService,
            FlowRecordService flowRecordService,
            FlowOperatorGateway flowOperatorGateway,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository
    ) {
        return new RepositoryHolderContextRegister(
                workflowService,
                flowRecordService,
                flowOperatorGateway,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }

    @Bean
    public FlowRecordService flowRecordService(FlowTodoRecordRepository flowTodoRecordRepository,FlowTodoMergeRepository flowTodoMergeRepository,FlowRecordRepository flowRecordRepository){
        return new FlowRecordService(flowTodoRecordRepository,flowTodoMergeRepository,flowRecordRepository);
    }

    @Bean
    public WorkflowService workflowService(WorkflowVersionRepository workflowVersionRepository,WorkflowRepository workflowRepository,WorkflowRuntimeRepository workflowRuntimeRepository){
        return new WorkflowService(workflowVersionRepository,workflowRepository,workflowRuntimeRepository);
    }

    @Bean
    public FlowService flowService(
            WorkflowService workflowService,
            FlowOperatorGateway flowOperatorGateway,
            FlowRecordService flowRecordService,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository
    ) {
        return new FlowService(
                workflowService,
                flowOperatorGateway,
                flowRecordService,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }
}
