package com.codingapi.flow.service.impl;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;
import com.codingapi.flow.context.LoopTriggerTraceContext;
import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.event.FlowRecordRevokeEvent;
import com.codingapi.flow.event.FlowSubProcessResetEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.exception.FlowValidationException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowSubProcessResetRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.SubProcessRepository;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.SubProcessStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 子流程数据重置服务（独立接口，不属于常规审批动作）。
 *
 * <p>重置以「已完成的子流程聚合记录」为目标：由选中的子流程实例流程id定位其所属聚合组，
 * 旧聚合组标记为已取代（保留审计），新聚合组由继承实例（未选中，沿用原结果）与
 * 重建实例（选中，基于旧实例数据从头重新发起的全新子流程）组成；当前记录作废，
 * 主流程退回子流程节点重新等待，新聚合组完成后经既有结果判定恢复主流程。</p>
 *
 * <p>可重置性由子流程节点的 {@link SubProcessStrategy#isResettable()} 配置控制，默认关闭；
 * 同时要求当前操作记录位于该子流程触发锚点的真实下游记录链上（条件/并行分支中
 * 不在该子流程之后的节点不可重置）。重建沿用子流程节点配置的创建脚本提交策略，
 * 不迁移、不跳过任何历史数据，历史聚合组与旧实例记录保持有效可查。</p>
 */
public class FlowSubProcessResetService {

    private final FlowSubProcessResetRequest request;
    private final IRepositoryHolder repositoryHolder;
    private final SubProcessRepository subProcessRepository;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

    public FlowSubProcessResetService(FlowSubProcessResetRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.repositoryHolder = repositoryHolder;
        this.subProcessRepository = repositoryHolder.getSubProcessRepository();
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
    }

    /**
     * 执行子流程数据重置。
     */
    public void reset() {
        request.verify();

        FlowRecord currentRecord = flowRecordService.getFlowRecord(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (!currentRecord.isTodo()) {
            throw FlowStateException.recordAlreadyDone();
        }
        if (currentRecord.getCurrentOperatorId() != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }

        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(currentRecord.getWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(currentRecord.getWorkRuntimeId() + " not found");
        }
        FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
        Workflow workflow = workflowRuntime.toWorkflow();

        SubProcessRecord currentGroup = loadTargetGroup(currentRecord.getProcessId(),
                request.getResetInstanceProcessIds());
        verifyResettableGroup(currentGroup);

        // 子流程节点配置开关：未开启重置能力的子流程不允许重置
        IFlowNode subProcessNode = workflow.getFlowNode(currentGroup.getNodeId());
        SubProcessStrategy strategy = subProcessNode.strategyManager().getStrategy(SubProcessStrategy.class);
        if (strategy == null || !strategy.isResettable()) {
            throw FlowStateException.subProcessNotSupportReset();
        }

        if (subProcessRepository.findByParentProcessId(currentRecord.getProcessId()).stream()
                .anyMatch(SubProcessRecord::isWaiting)) {
            throw FlowStateException.subProcessResetWaiting();
        }

        FlowRecord anchorRecord = repositoryHolder.getRecordById(currentGroup.getParentRecordId());
        if (anchorRecord == null) {
            throw FlowNotFoundException.record(currentGroup.getParentRecordId());
        }
        // 锁定合并节点守卫：重置仅允许在锁定合并节点（锚点之后首个产生业务记录的节点）的待办上发起。
        // 并行/包容分支扇出时多分支共享锚点，仅按记录产生顺序锁定的第一个分支节点可重置，
        // 兄弟分支与合并节点之后的更深层节点均不允许
        FlowRecord lockedMergeRecord = findLockedMergeRecord(currentRecord.getProcessId(), anchorRecord.getId());
        if (lockedMergeRecord == null
                || !currentRecord.getNodeId().equals(lockedMergeRecord.getNodeId())) {
            throw FlowStateException.subProcessResetMergeNodeOnly();
        }

        // 先创建重建实例（不触发自动提交），落库新聚合组后再统一提交，
        // 避免重建实例在新组落库前结束导致结果判定找不到聚合组
        boolean submit = strategy.isSubmit();
        FlowService flowService = repositoryHolder.createFlowService();
        List<SubProcessRecord.Instance> newInstances = new ArrayList<>();
        List<FlowActionRequest> submitRequests = new ArrayList<>();
        for (SubProcessRecord.Instance instance : currentGroup.getInstances()) {
            if (request.getResetInstanceProcessIds().contains(instance.getProcessId())) {
                FlowCreateRequest createRequest = buildRebuildRequest(instance, anchorRecord);
                long newRecordId = flowService.create(createRequest);
                FlowRecord newStartRecord = repositoryHolder.getRecordById(newRecordId);
                newInstances.add(SubProcessRecord.Instance.rebuiltFrom(newStartRecord, instance.getProcessId()));
                if (submit) {
                    submitRequests.add(createRequest.toActionRequest(newRecordId));
                }
            } else {
                newInstances.add(SubProcessRecord.Instance.inheritFrom(instance));
            }
        }

        currentGroup.supersede();
        subProcessRepository.save(currentGroup);
        SubProcessRecord newGroup = new SubProcessRecord(
                FlowIDGeneratorGatewayContext.getInstance().generateProcessId(),
                anchorRecord,
                currentGroup.getNodeId(),
                newInstances);
        subProcessRepository.save(newGroup);

        for (FlowActionRequest submitRequest : submitRequests) {
            flowService.action(submitRequest);
        }

        // 作废触发锚点之后的记录链（复用撤销语义）：重置后主流程退回子流程节点等待，
        // 锚点之后的旧记录不再代表有效路径，且其 fromId + 节点与恢复后新建记录相同，
        // 不作废会被同节点记录查询误判为多人审批
        List<FlowRecord> invalidatedRecords = new ArrayList<>();
        List<FlowRecord> afterRecords = repositoryHolder
                .findAfterRecords(currentRecord.getProcessId(), anchorRecord.getId());
        for (FlowRecord afterRecord : afterRecords) {
            if (afterRecord.getId() == currentRecord.getId()) {
                // 重置说明记录在操作记录上，供审计追溯
                afterRecord.setAdvice(request.getAdvice());
            }
            afterRecord.revoke();
            invalidatedRecords.add(afterRecord);
        }
        repositoryHolder.saveRecords(invalidatedRecords);
        // 重置属于有意的退回重走：清除该流程的循环触发标记，
        // 避免重走再次触发下游节点（如抄送）时被被动式环检测误判为循环
        LoopTriggerTraceContext.getInstance().clearByProcess(currentRecord.getProcessId());

        boolean mock = repositoryHolder instanceof MockRepositoryHolder;
        IFlowOperator resetOperator = repositoryHolder.getOperatorById(request.getOperatorId());
        for (FlowRecord invalidatedRecord : invalidatedRecords) {
            EventPusher.push(new FlowRecordRevokeEvent(invalidatedRecord, mock));
        }
        EventPusher.push(new FlowSubProcessResetEvent(
                currentGroup.snapshot(),
                newGroup.snapshot(),
                currentRecord.getId(),
                resetOperator,
                mock));
    }

    /**
     * 由选中的子流程实例流程id定位其所属的聚合组（全部选中实例须同属一个未取代的聚合组）。
     *
     * @param processId       主流程的流程id
     * @param selectedProcessIds 选中重建的实例流程id
     * @return 聚合组；实例不存在时抛出参数异常
     */
    private SubProcessRecord loadTargetGroup(String processId, List<String> selectedProcessIds) {
        List<SubProcessRecord> groups = subProcessRepository.findByParentProcessId(processId);
        SubProcessRecord targetGroup = null;
        for (String selectedProcessId : selectedProcessIds) {
            SubProcessRecord owner = groups.stream()
                    .filter(group -> !group.isSuperseded())
                    .filter(group -> group.containsChildProcess(selectedProcessId))
                    .findFirst()
                    .orElse(null);
            if (owner == null) {
                throw FlowValidationException.resetInstanceNotFound(selectedProcessId);
            }
            if (targetGroup == null) {
                targetGroup = owner;
            } else if (targetGroup.getId() != owner.getId()) {
                throw FlowValidationException.resetInstanceNotFound(selectedProcessId);
            }
        }
        return targetGroup;
    }

    /**
     * 校验聚合组可重置：未被取代、已放行且全部实例已结束。
     */
    private void verifyResettableGroup(SubProcessRecord group) {
        if (group == null || group.isSuperseded() || !group.isPassed() || !group.isAllFinished()) {
            throw FlowStateException.subProcessNotSupportReset();
        }
    }

    /**
     * 查找锁定的合并节点记录：锚点记录之后的首代业务记录中，按记录产生顺序取第一条。
     *
     * <p>恢复后的记录遍历以锚点为来源记录，合并点首代记录均满足 {@code fromId == 锚点id}：
     * 串联/条件分支/触发与抄送直通时唯一；并行/包容分支扇出时为多个分支首节点记录，
     * 按记录id（即遍历产生顺序）取第一个分支节点作为锁定合并节点。</p>
     *
     * @param processId 主流程的流程id
     * @param anchorId  锚点记录id
     * @return 锁定合并节点记录；不存在时返回 null
     */
    private FlowRecord findLockedMergeRecord(String processId, long anchorId) {
        return repositoryHolder.findAfterRecords(processId, anchorId).stream()
                .filter(record -> record.getFromId() == anchorId)
                .filter(record -> !record.isNotify())
                .min(Comparator.comparingLong(FlowRecord::getId))
                .orElse(null);
    }

    /**
     * 依据旧实例的启动记录反推重建请求：沿用原子流程定义、表单数据、流程标题与原发起人，
     * 锚定到原触发记录。
     *
     * @param oldInstance  被重置的旧实例
     * @param anchorRecord 父流程中子流程节点的触发记录
     * @return 重建子流程的创建请求
     */
    private FlowCreateRequest buildRebuildRequest(SubProcessRecord.Instance oldInstance, FlowRecord anchorRecord) {
        FlowRecord oldStartRecord = repositoryHolder.getRecordById(oldInstance.getStartRecordId());
        if (oldStartRecord == null) {
            throw FlowNotFoundException.record(oldInstance.getStartRecordId());
        }
        FlowCreateRequest request = new FlowCreateRequest();
        request.setWorkCode(oldStartRecord.getWorkCode());
        request.setOperatorId(oldStartRecord.getCreateOperatorId());
        request.setActionId(oldStartRecord.getActionId());
        request.setFormData(oldStartRecord.getFormData());
        request.setWorkTitle(oldInstance.getWorkTitle());
        request.setParentRecordId(anchorRecord.getId());
        return request;
    }

    /**
     * 判断指定记录当前是否可以执行子流程重置（供详情数据计算重置标识）。
     *
     * <p>满足全部条件时返回 true：记录为待办、所属流程存在未取代且已放行、全部实例已结束的
     * 聚合组，该子流程节点配置了重置能力，且记录位于锁定合并节点（锚点之后首个产生业务
     * 记录的节点，并行/包容分支扇出时按记录产生顺序取第一个分支节点）。</p>
     *
     * @param record           当前记录
     * @param repositoryHolder 仓储持有者
     * @return 可重置返回 true
     */
    public static boolean canReset(FlowRecord record, IRepositoryHolder repositoryHolder) {
        if (record == null || record.getId() <= 0 || !record.isTodo()) {
            return false;
        }
        SubProcessRepository subProcessRepository = repositoryHolder.getSubProcessRepository();
        List<SubProcessRecord> groups = subProcessRepository.findByParentProcessId(record.getProcessId());
        if (groups.stream().anyMatch(SubProcessRecord::isWaiting)) {
            return false;
        }
        WorkflowService workflowService = repositoryHolder.getWorkflowService();
        for (SubProcessRecord group : groups) {
            if (group.isSuperseded() || !group.isPassed() || !group.isAllFinished()) {
                continue;
            }
            WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(group.getParentWorkRuntimeId());
            if (workflowRuntime == null) {
                continue;
            }
            Workflow workflow = workflowRuntime.toWorkflow();
            IFlowNode subProcessNode = workflow.getFlowNode(group.getNodeId());
            if (subProcessNode == null) {
                continue;
            }
            SubProcessStrategy strategy = subProcessNode.strategyManager().getStrategy(SubProcessStrategy.class);
            if (strategy == null || !strategy.isResettable()) {
                continue;
            }
            FlowRecord anchorRecord = repositoryHolder.getRecordById(group.getParentRecordId());
            if (anchorRecord == null) {
                continue;
            }
            FlowRecord lockedMergeRecord = findLockedMergeRecord(record.getProcessId(),
                    anchorRecord.getId(), repositoryHolder);
            if (lockedMergeRecord != null && record.getNodeId().equals(lockedMergeRecord.getNodeId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找锁定的合并节点记录（静态版，供 {@link #canReset} 使用），
     * 语义与 {@link #findLockedMergeRecord(String, long)} 一致。
     */
    private static FlowRecord findLockedMergeRecord(String processId, long anchorId,
                                                    IRepositoryHolder repositoryHolder) {
        return repositoryHolder.findAfterRecords(processId, anchorId).stream()
                .filter(record -> record.getFromId() == anchorId)
                .filter(record -> !record.isNotify())
                .min(Comparator.comparingLong(FlowRecord::getId))
                .orElse(null);
    }
}
