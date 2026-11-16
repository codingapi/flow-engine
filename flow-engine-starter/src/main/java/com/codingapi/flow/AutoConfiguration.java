package com.codingapi.flow;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.event.IFlowEventPublisher;
import com.codingapi.flow.event.SpringFlowEventPublisher;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.register.NodeViewJavaScriptCacheContextRegister;
import com.codingapi.flow.register.RepositoryHolderContextRegister;
import com.codingapi.flow.register.FlowScriptContextRegister;
import com.codingapi.flow.register.GatewayContextRegister;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.runner.FlowDelayTaskRunner;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoConfiguration {


    @Bean
    public NodeViewJavaScriptCacheContextRegister nodeViewJavaScriptCacheContextRegister(NodeViewJavaScriptRepository nodeViewJavaScriptRepository){
        return new NodeViewJavaScriptCacheContextRegister(nodeViewJavaScriptRepository);
    }


    @Bean
    public FlowDelayTaskRunner delayTaskRunner(RepositoryHolderContextRegister repositoryHolderContextRegister) {
        return new FlowDelayTaskRunner(RepositoryHolderContext.getInstance());
    }

    @Bean
    public GatewayContextRegister gatewayContextRegister(FlowOperatorGateway flowOperatorGateway) {
        return new GatewayContextRegister(flowOperatorGateway);
    }

    /**
     * 流程事件发布器：事务提交后投递 + 按流程id分区串行（issue #274）。
     * 可通过 codingapi.flow.event.ordered=false 关闭，关闭后回退为直接推送到事件框架。
     */
    @Bean
    @ConditionalOnMissingBean(IFlowEventPublisher.class)
    @ConditionalOnProperty(value = "codingapi.flow.event.ordered", matchIfMissing = true)
    public SpringFlowEventPublisher flowEventPublisher(ApplicationContext applicationContext) {
        return new SpringFlowEventPublisher(applicationContext);
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
            ParallelBranchRepository parallelBranchRepository,
            DelayTaskRepository delayTaskRepository,
            UrgeIntervalRepository urgeIntervalRepository,
            FlowOperatorAssignmentRepository flowOperatorAssignmentRepository,
            SubProcessRepository subProcessRepository,
            GatewayContextRegister gatewayContextRegister
    ) {
        return new RepositoryHolderContextRegister(
                workflowService,
                flowRecordService,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository,
                flowOperatorAssignmentRepository,
                subProcessRepository,
                gatewayContextRegister
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
    public FlowService flowService(RepositoryHolderContextRegister repositoryHolderContextRegister) {
        return new FlowService(RepositoryHolderContext.getInstance());
    }
}
