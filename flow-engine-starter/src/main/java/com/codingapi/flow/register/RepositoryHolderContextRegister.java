package com.codingapi.flow.register;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;

@AllArgsConstructor
public class RepositoryHolderContextRegister implements InitializingBean {

    private final WorkflowRepository workflowRepository;
    private final WorkflowBackupRepository workflowBackupRepository;
    private final FlowRecordRepository flowRecordRepository;
    private final FlowOperatorGateway flowOperatorGateway;
    private final ParallelBranchRepository parallelBranchRepository;
    private final DelayTaskRepository delayTaskRepository;
    private final UrgeIntervalRepository urgeIntervalRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

        RepositoryHolderContext.getInstance().register(
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
