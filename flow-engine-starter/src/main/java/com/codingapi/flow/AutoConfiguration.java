package com.codingapi.flow;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.register.FlowRepositoryRegister;
import com.codingapi.flow.register.FlowScriptContextRegister;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.runner.FlowDelayTaskRunner;
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
    public FlowScriptContextRegister flowScriptContextRegister(
            ApplicationContext spring,
            FlowOperatorGateway flowOperatorGateway,
            FlowRecordRepository flowRecordRepository
            ) {
        return new FlowScriptContextRegister(spring, flowOperatorGateway,flowRecordRepository);
    }

    @Bean
    public FlowRepositoryRegister flowRepositoryRegister(
            WorkflowRepository workflowRepository,
            WorkflowBackupRepository workflowBackupRepository,
            FlowRecordRepository flowRecordRepository,
            FlowOperatorGateway flowOperatorGateway,
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository
    ) {
        return new FlowRepositoryRegister(
                workflowRepository,
                workflowBackupRepository,
                flowRecordRepository,
                flowOperatorGateway,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }
}
