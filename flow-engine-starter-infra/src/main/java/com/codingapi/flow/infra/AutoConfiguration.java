package com.codingapi.flow.infra;

import com.codingapi.flow.infra.jpa.*;
import com.codingapi.flow.infra.repository.impl.*;
import com.codingapi.flow.repository.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.codingapi.flow.infra.entity")
@EnableJpaRepositories(basePackages = "com.codingapi.flow.infra.jpa")
public class AutoConfiguration {

    @Bean
    public DelayTaskRepository delayTaskRepository(DelayTaskEntityRepository delayTaskEntityRepository){
        return new DelayTaskRepositoryImpl(delayTaskEntityRepository);
    }

    @Bean
    public UrgeIntervalRepository urgeIntervalRepository(UrgeIntervalEntityRepository urgeIntervalEntityRepository){
        return new UrgeIntervalRepositoryImpl(urgeIntervalEntityRepository);
    }

    @Bean
    public ParallelBranchRepository parallelBranchRepository(ParallelControlEntityRepository parallelControlEntityRepository){
        return new ParallelBranchRepositoryImpl(parallelControlEntityRepository);
    }

    @Bean
    public WorkflowRepository workflowRepository(WorkflowEntityRepository workflowEntityRepository){
        return new WorkflowRepositoryImpl(workflowEntityRepository);
    }

    @Bean
    public WorkflowBackupRepository workflowBackupRepository(WorkflowBackupEntityRepository workflowBackupEntityRepository){
        return new WorkflowBackupRepositoryImpl(workflowBackupEntityRepository);
    }

    @Bean
    public FlowRecordRepository flowRecordRepository(FlowRecordEntityRepository flowRecordEntityRepository){
        return new FlowRecordRepositoryImpl(flowRecordEntityRepository);
    }

    @Bean
    public FlowTodoRecordRepository flowTodoRecordRepository(FlowTodoRecordEntityRepository flowTodoRecordEntityRepository){
        return new FlowTodoRecordRepositoryImpl(flowTodoRecordEntityRepository);
    }

    @Bean
    public FlowTodoMergeRepository flowTodoMargeRepository(FlowTodoMargeEntityRepository flowTodoMargeEntityRepository){
        return new FlowTodoMergeRepositoryImpl(flowTodoMargeEntityRepository);
    }

}
