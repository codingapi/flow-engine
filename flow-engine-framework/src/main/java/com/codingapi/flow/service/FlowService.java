package com.codingapi.flow.service;

import com.codingapi.flow.context.RepositoryHolderContext;
import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.pojo.request.*;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.repository.*;
import com.codingapi.flow.service.impl.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程服务
 */
@Transactional
public class FlowService {

    public FlowService(WorkflowService workflowService,
                       FlowOperatorGateway flowOperatorGateway,
                       FlowRecordService flowRecordService,
                       ParallelBranchRepository parallelBranchRepository,
                       DelayTaskRepository delayTaskRepository,
                       UrgeIntervalRepository urgeIntervalRepository) {
        RepositoryHolderContext.getInstance()
                .register(workflowService,
                        flowRecordService,
                        flowOperatorGateway,
                        parallelBranchRepository,
                        delayTaskRepository,
                        urgeIntervalRepository);
    }

    /**
     * 流程详情
     * @param request 流程详情请求
     * @return 流程详情
     */
    public FlowContent detail(FlowDetailRequest request) {
        FlowDetailService flowDetailService = new FlowDetailService(request);
        return flowDetailService.detail();
    }



    /**
     * 流程节点记录
     * @param request 流程节点记录请求
     * @return 流程节点记录列表
     */
    public List<ProcessNode> processNodes(FlowProcessNodeRequest request) {
        FlowProcessNodeService flowProcessNodeService = new FlowProcessNodeService(request);
        return flowProcessNodeService.processNodes();
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
