package com.codingapi.flow.service;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.impl.*;

/**
 * 流程服务
 */
public class FlowService {

    public FlowService(WorkflowRepository workflowRepository,
                       FlowOperatorGateway flowOperatorGateway,
                       FlowRecordRepository flowRecordRepository,
                       FlowTodoRecordRepository flowTodoRecordRepository,
                       FlowTodoMargeRepository flowTodoMargeRepository,
                       WorkflowBackupRepository workflowBackupRepository,
                       ParallelBranchRepository parallelBranchRepository,
                       DelayTaskRepository delayTaskRepository,
                       UrgeIntervalRepository urgeIntervalRepository) {
        RepositoryHolderContext.getInstance()
                .register(workflowRepository,
                        workflowBackupRepository,
                        flowRecordRepository,
                        flowTodoRecordRepository,
                        flowTodoMargeRepository,
                        flowOperatorGateway,
                        parallelBranchRepository,
                        delayTaskRepository,
                        urgeIntervalRepository);
    }

    /**
     * 流程详情
     * @param id 流程id 或者workflowCode
     * @param currentOperator 当前操作人
     * @return 流程详情
     */
    public FlowContent detail(String id,IFlowOperator currentOperator) {
        FlowDetailService flowDetailService = new FlowDetailService(id,currentOperator);
        return flowDetailService.detail();
    }

    /**
     * 创建流程
     *
     * @param request 创建流程请求
     * @return 创建的流程id
     */
    public long create(FlowCreateRequest request) {
        FlowCreateService flowCreateService = new FlowCreateService(request);
        return flowCreateService.create();
    }

    /**
     * 流程审批
     *
     * @param request 审批请求
     */
    public void action(FlowActionRequest request) {
        FlowActionService flowActionService = new FlowActionService(request);
        flowActionService.action();
    }

    /**
     * 撤销流程
     *
     * @param request 撤销请求
     */
    public void revoke(FlowRevokeRequest request) {
        FlowRevokeService flowRevokeService = new FlowRevokeService(request);
        flowRevokeService.revoke();
    }


    /**
     * 催办
     */
    public void urge(FlowUrgeRequest request) {
        FlowUrgeService flowUrgeService = new FlowUrgeService(request);
        flowUrgeService.urge();
    }
}
