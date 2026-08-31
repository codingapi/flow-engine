package com.codingapi.flow.service.impl;

import com.codingapi.flow.cache.FlowRuntimeScriptLocalCache;

import com.codingapi.flow.domain.SubProcessRecord;
import com.codingapi.flow.event.FlowRecordRevokeEvent;
import com.codingapi.flow.event.FlowRecordTodoEvent;
import com.codingapi.flow.event.IFlowEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.manager.NodeStrategyManager;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.strategy.node.RevokeStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.springboot.framework.event.EventPusher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 撤销流程服务
 */
public class FlowRevokeService {

    private final FlowRevokeRequest request;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;
    private final IRepositoryHolder repositoryHolder;

    public FlowRevokeService(FlowRevokeRequest request,IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
        this.repositoryHolder = repositoryHolder;
    }

    public void revoke() {
        request.verify();
        // 验证当前用户
        FlowRecord currentRecord = flowRecordService.getFlowRecord(request.getRecordId());
        if (currentRecord == null) {
            throw FlowNotFoundException.record(request.getRecordId());
        }
        if (currentRecord.isTodo()) {
            throw FlowStateException.recordAlreadyTodo();
        }
        if (currentRecord.isFinish()) {
            throw FlowStateException.recordNotSupportRevoke();
        }
        // 自动办结记录（无审批动作的已办：相同人员自动通过 / 或签并签遗留待办）从未产生过待办，
        // 不存在"撤回自己办理的审批"的语义，不支持撤销（issue #226）
        if (currentRecord.isAutoDone()) {
            throw FlowStateException.recordNotSupportRevoke();
        }
        boolean waitingSubProcess = repositoryHolder.getSubProcessRepository()
                .findByParentRecordId(currentRecord.getId()).stream()
                .anyMatch(SubProcessRecord::isWaiting);
        if (waitingSubProcess) {
            throw FlowStateException.recordNotSupportRevoke();
        }
        long currentOperatorId = currentRecord.getCurrentOperatorId();
        if (currentOperatorId != request.getOperatorId()) {
            throw FlowStateException.operatorNotMatch();
        }
        WorkflowRuntime workflowRuntime = workflowService.getWorkflowRuntime(currentRecord.getWorkRuntimeId());
        if (workflowRuntime == null) {
            throw FlowNotFoundException.workflow(currentRecord.getWorkRuntimeId() + " not found");
        }
        FlowRuntimeScriptLocalCache.getInstance().set(workflowRuntime.getScripts());
        Workflow workflow = workflowRuntime.toWorkflow();
        IFlowNode currentNode = workflow.getFlowNode(currentRecord.getNodeId());
        NodeStrategyManager nodeStrategyManager = currentNode.strategyManager();
        RevokeStrategy revokeStrategy = nodeStrategyManager.getStrategy(RevokeStrategy.class);
        if (revokeStrategy == null || !revokeStrategy.isEnable()) {
            throw FlowStateException.nodeNotSupportRevoke();
        }

        List<FlowRecord> afterRecords = flowRecordService.findFlowRecordAfterRecords(currentRecord.getProcessId(), currentRecord.getId());
        // 退回下级记录, 如果下级记录都完成则不允许退回。
        // 计算有效直接后继时，自动办结记录（isAutoDone：相同人员自动通过的留痕记录）视为透明节点，
        // 沿 fromId 链向下穿透到真实后继——否则自动通过留痕会让"下级已办"误判成立，
        // 在更下游仍有真实待办时永久阻断撤回（issue #226）。
        // 仅当透明记录确有下游后继时才穿透，或签/并签遗留的 autoDone 记录无后继，保持原判。
        if (revokeStrategy.isRemoveNext()) {
            List<FlowRecord> nextRecords = new ArrayList<>();
            Set<Long> frontier = new HashSet<>();
            frontier.add(currentRecord.getId());
            while (!frontier.isEmpty()) {
                Set<Long> currentFrontier = frontier;
                List<FlowRecord> directRecords = afterRecords.stream()
                        .filter(flowRecord -> currentFrontier.contains(flowRecord.getFromId()))
                        .toList();
                boolean hasTodo = false;
                Set<Long> nextFrontier = new HashSet<>();
                for (FlowRecord directRecord : directRecords) {
                    if (directRecord.isAutoDone()) {
                        nextFrontier.add(directRecord.getId());
                    } else {
                        nextRecords.add(directRecord);
                        if (directRecord.isTodo()) {
                            hasTodo = true;
                        }
                    }
                }
                if (hasTodo) {
                    break;
                }
                frontier = nextFrontier;
            }
            boolean nextRecordDone = true;
            for (FlowRecord nextRecord : nextRecords) {
                if (nextRecord.isTodo()) {
                    nextRecordDone = false;
                }
            }
            if (nextRecordDone) {
                throw FlowStateException.recordNotSupportRevoke();
            }
        }

        List<FlowRecord> recordList = new ArrayList<>();
        List<IFlowEvent> flowEvents = new ArrayList<>();

        currentRecord.clearDone();

        recordList.add(currentRecord);
        flowEvents.add(new FlowRecordTodoEvent(currentRecord,repositoryHolder instanceof MockRepositoryHolder));

        for (FlowRecord afterRecord : afterRecords) {
            afterRecord.revoke();
            recordList.add(afterRecord);
            // 撤销事件,登记撤销发起的记录数据
            flowEvents.add(new FlowRecordRevokeEvent(afterRecord, repositoryHolder instanceof MockRepositoryHolder));
        }

        repositoryHolder.saveRecords(recordList);
        flowEvents.forEach(EventPusher::push);

    }
}
