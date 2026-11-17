package com.codingapi.flow.service;

import com.codingapi.flow.cache.FlowOperatorLocalThreadCache;
import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.pojo.request.*;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.pojo.response.FlowContent;
import com.codingapi.flow.pojo.response.FlowForceSyncResult;
import com.codingapi.flow.pojo.response.ProcessNode;
import com.codingapi.flow.service.impl.*;
import com.codingapi.flow.session.IRepositoryHolder;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程服务
 */
@Transactional
public class FlowService {

    @Getter
    private final IRepositoryHolder repositoryHolder;

    public FlowService(IRepositoryHolder repositoryHolder) {
        this.repositoryHolder = repositoryHolder;
    }

    /**
     * 流程详情
     *
     * @param request 流程详情请求
     * @return 流程详情
     */
    public FlowContent detail(FlowDetailRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowDetailService flowDetailService = new FlowDetailService(request, this.repositoryHolder);
        return flowDetailService.detail();
    }


    /**
     * 流程节点记录
     *
     * @param request 流程节点记录请求
     * @return 流程节点记录列表
     */
    public List<ProcessNode> processNodes(FlowProcessNodeRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowProcessNodeService flowProcessNodeService = new FlowProcessNodeService(request, this.repositoryHolder);
        return flowProcessNodeService.processNodes();
    }

    /**
     * 创建流程
     *
     * @param request 创建流程请求
     * @return 创建的流程id
     */
    public long create(FlowCreateRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowCreateService flowCreateService = new FlowCreateService(request, this.repositoryHolder);
        return flowCreateService.create();
    }

    /**
     * 流程审批
     *
     * @param request 审批请求
     */
    public ActionResponse action(FlowActionRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowActionService flowActionService = new FlowActionService(request, this.repositoryHolder);
        return flowActionService.action();
    }

    /**
     * 撤销流程
     *
     * @param request 撤销请求
     */
    public void revoke(FlowRevokeRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowRevokeService flowRevokeService = new FlowRevokeService(request, this.repositoryHolder);
        flowRevokeService.revoke();
    }


    /**
     * 删除流程
     * <p>
     * 仅用于作废尚未流转(位于开始节点且无后续记录)的流程实例
     * @param request 删除请求
     */
    public void delete(FlowDeleteRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowDeleteService flowDeleteService = new FlowDeleteService(request, this.repositoryHolder);
        flowDeleteService.delete();
    }


    /**
     * 催办
     */
    public void urge(FlowUrgeRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowUrgeService flowUrgeService = new FlowUrgeService(request, this.repositoryHolder);
        flowUrgeService.urge();
    }

    /**
     * 子流程数据重置（独立接口，不属于常规审批动作）。
     * <p>
     * 将已完成的子流程聚合记录中的选中实例从头重建为新子流程，主流程退回子流程节点重新等待。
     * 仅当子流程节点配置了重置能力（{@code SubProcessStrategy.resettable}）时允许执行。
     *
     * @param request 重置请求
     */
    public void resetSubProcess(FlowSubProcessResetRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowSubProcessResetService flowSubProcessResetService =
                new FlowSubProcessResetService(request, this.repositoryHolder);
        flowSubProcessResetService.reset();
    }

    /**
     * 已完成流程二次发起（独立接口，不属于常规审批动作）。
     * <p>
     * 复制已正常完成的流程记录的表单数据重新创建一条流程，新流程停在开始节点成为发起人的待办，
     * 由发起人调整数据后重新提交执行；源流程记录不做任何改写。仅流程管理员可执行。
     *
     * @param request 二次发起请求
     * @return 新流程的开始节点记录id
     */
    public long reissue(FlowReissueRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowReissueService flowReissueService = new FlowReissueService(request, this.repositoryHolder);
        return flowReissueService.reissue();
    }

    /**
     * 运行中流程实例强制同步最新流程设计（独立接口，不属于常规审批动作）。
     * <p>
     * 仅对请求中选中的运行实例生效，把其流程记录的运行时快照引用换绑为最新设计对应的快照；
     * 未选中的实例保持原快照。同步前会校验结构一致性（不允许新增/删除节点、改变节点类型/层级/顺序，
     * 旧按钮动作必须保留），表单字段不做校验。
     *
     * @param request 强制同步请求
     * @return 同步结果
     */
    public FlowForceSyncResult forceSync(FlowForceSyncRequest request) {
        FlowOperatorLocalThreadCache.getInstance().clear();
        FlowRuntimeScriptLocalCache.getInstance().clear();
        FlowForceSyncService flowForceSyncService = new FlowForceSyncService(request, this.repositoryHolder);
        return flowForceSyncService.sync();
    }
}
