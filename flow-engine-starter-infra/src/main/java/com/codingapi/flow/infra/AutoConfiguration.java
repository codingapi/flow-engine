package com.codingapi.flow.infra;

import com.codingapi.flow.infra.jpa.*;
import com.codingapi.flow.infra.repository.impl.*;
import com.codingapi.flow.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@Import(FlowJpaPackageRegistrar.class)
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
    public WorkflowRuntimeRepository workflowRuntimeRepository(WorkflowRuntimeEntityRepository workflowRuntimeEntityRepository){
        return new WorkflowRuntimeRepositoryImpl(workflowRuntimeEntityRepository);
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
