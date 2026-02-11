package com.codingapi.flow;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.register.RepositoryHolderContextRegister;
import com.codingapi.flow.register.FlowScriptContextRegister;
import com.codingapi.flow.register.GatewayContextRegister;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.runner.FlowDelayTaskRunner;
import com.codingapi.flow.service.FlowService;
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
            WorkflowRepository workflowRepository,
            WorkflowBackupRepository workflowBackupRepository,
            FlowRecordRepository flowRecordRepository,
            FlowOperatorGateway flowOperatorGateway,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository
    ) {
        return new RepositoryHolderContextRegister(
                workflowRepository,
                workflowBackupRepository,
                flowRecordRepository,
                flowOperatorGateway,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }

    @Bean
    public FlowService flowService(
            WorkflowRepository workflowRepository,
            FlowOperatorGateway flowOperatorGateway,
            FlowRecordRepository flowRecordRepository,
            WorkflowBackupRepository workflowBackupRepository,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository
    ) {
        return new FlowService(
                workflowRepository,
                flowOperatorGateway,
                flowRecordRepository,
                workflowBackupRepository,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }
}
