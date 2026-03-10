package com.codingapi.flow.register;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.InitializingBean;

@AllArgsConstructor
public class RepositoryHolderContextRegister implements InitializingBean {

    private final WorkflowService workflowService;
    private final FlowRecordService flowRecordService;
    private final FlowOperatorGateway flowOperatorGateway;
    private final ParallelBranchRepository parallelBranchRepository;
    private final DelayTaskRepository delayTaskRepository;
    private final UrgeIntervalRepository urgeIntervalRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

        RepositoryHolderContext.getInstance().register(
                workflowService,
                flowRecordService,
                flowOperatorGateway,
                parallelBranchRepository,
                delayTaskRepository,
                urgeIntervalRepository
        );
    }
}
