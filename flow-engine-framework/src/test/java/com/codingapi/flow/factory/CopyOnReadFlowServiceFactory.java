package com.codingapi.flow.factory;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.repository.CopyOnReadFlowRecordRepository;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;

/**
 * 使用 copy-on-read 流程记录仓储的测试工厂，模拟生产 JPA 仓储的对象隔离语义，
 * 用于复现 issue #184 主流程最终状态被覆盖回运行中的问题。
 */
public class CopyOnReadFlowServiceFactory extends MyFlowServiceFactory {

    /**
     * 覆盖父类的记录仓储，读写均基于最近一次保存状态的快照。
     */
    public CopyOnReadFlowRecordRepository flowRecordRepository;

    public CopyOnReadFlowServiceFactory() {
        super();
        // 用快照语义仓储重建记录服务并重新注册，确保流程引擎全部走该仓储
        flowRecordRepository = new CopyOnReadFlowRecordRepository();
        flowRecordService = new FlowRecordService(flowTodoRecordRepository, flowTodoMergeRepository, flowRecordRepository);
        RepositoryHolderContext.getInstance().register(workflowService, flowRecordService, parallelBranchRepository,
                delayTaskRepository, urgeIntervalRepository, flowOperatorAssignmentRepository, subProcessRepository);
        repositoryHolder = RepositoryHolderContext.getInstance();
        this.flowService = new FlowService(this.repositoryHolder);
    }
}