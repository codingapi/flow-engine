package com.codingapi.flow.service.impl;

import com.codingapi.flow.event.FlowEventPublisher;
import com.codingapi.flow.event.FlowRecordForceSyncEvent;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.FlowPermissionException;
import com.codingapi.flow.exception.FlowStateException;
import com.codingapi.flow.mock.MockRepositoryHolder;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.pojo.request.FlowForceSyncRequest;
import com.codingapi.flow.pojo.response.FlowForceSyncResult;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.service.FlowRecordService;
import com.codingapi.flow.service.WorkflowService;
import com.codingapi.flow.session.IRepositoryHolder;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import com.codingapi.flow.workflow.runtime.WorkflowStructureDiff;
import com.codingapi.flow.workflow.runtime.WorkflowStructureVerifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行中流程实例强制同步最新流程设计服务（issue #281，独立接口，不属于常规审批动作）。
 *
 * <p>流程实例启动后以启动时的 {@link WorkflowRuntime} 快照执行，设计侧的后续修改不会影响运行实例。
 * 本服务用于把选中的运行实例换绑到「最新设计对应的快照」上：</p>
 *
 * <ul>
 *     <li>只处理请求中选中的流程实例，未选中的实例仍使用原快照，不受影响；</li>
 *     <li>不改写任何已有快照：最新设计按 {@code (workId, updatedTime)} 生成或复用快照，
 *     未同步的实例继续读取原快照；</li>
 *     <li>同步前逐实例校验结构一致性（节点集合、类型、层级、顺序、已有按钮动作），
 *     任一实例不通过则整批拒绝；</li>
 *     <li>换绑后按新的合并键重建待办，保证待办列表与合并关系一致。</li>
 * </ul>
 */
public class FlowForceSyncService {

    private final FlowForceSyncRequest request;
    private final IRepositoryHolder repositoryHolder;
    private final FlowRecordService flowRecordService;
    private final WorkflowService workflowService;

    public FlowForceSyncService(FlowForceSyncRequest request, IRepositoryHolder repositoryHolder) {
        this.request = request;
        this.repositoryHolder = repositoryHolder;
        this.flowRecordService = repositoryHolder.getFlowRecordService();
        this.workflowService = repositoryHolder.getWorkflowService();
    }

    public FlowForceSyncResult sync() {
        request.verify();

        Workflow latestWorkflow = workflowService.getWorkflowByCode(request.getWorkCode());
        if (latestWorkflow == null) {
            throw FlowNotFoundException.workflow(request.getWorkCode());
        }
        // 最新设计必须通过校验，避免把不合法的设计同步给运行实例
        latestWorkflow.verify();

        IFlowOperator operator = repositoryHolder.getOperatorById(request.getOperatorId());
        if (operator == null) {
            throw FlowNotFoundException.operator(request.getOperatorId());
        }
        // 强制同步是流程管理员对运行中实例的高风险干预操作，仅流程管理员可执行
        if (!operator.isFlowManager()) {
            throw FlowPermissionException.accessDenied("force sync flow instance");
        }

        Map<String, List<FlowRecord>> recordsByProcess = this.loadRunningInstances();

        // 最新设计对应的快照（同一设计版本下多个实例共享，未同步实例不受影响）
        WorkflowRuntime latestRuntime = workflowService.getOrCreateWorkflowRuntime(latestWorkflow);
        long newRuntimeId = latestRuntime.getId();
        Workflow latestSnapshot = latestRuntime.toWorkflow();

        List<String> targetProcessIds = new ArrayList<>();
        List<String> skippedProcessIds = new ArrayList<>();
        for (Map.Entry<String, List<FlowRecord>> entry : recordsByProcess.entrySet()) {
            String processId = entry.getKey();
            long oldRuntimeId = entry.getValue().get(0).getWorkRuntimeId();
            if (oldRuntimeId == newRuntimeId) {
                // 已使用最新快照，幂等跳过
                skippedProcessIds.add(processId);
                continue;
            }
            WorkflowRuntime oldRuntime = workflowService.getWorkflowRuntime(oldRuntimeId);
            if (oldRuntime == null) {
                throw FlowNotFoundException.workflow(String.valueOf(oldRuntimeId));
            }
            WorkflowStructureDiff diff = WorkflowStructureVerifier.compare(oldRuntime.toWorkflow(), latestSnapshot);
            if (!diff.isConsistent()) {
                throw FlowStateException.forceSyncStructureInconsistent(processId, diff.describe());
            }
            targetProcessIds.add(processId);
        }

        if (targetProcessIds.isEmpty()) {
            return new FlowForceSyncResult(newRuntimeId, latestRuntime.getWorkVersion(), List.of(), skippedProcessIds);
        }

        boolean mock = repositoryHolder instanceof MockRepositoryHolder;
        for (String processId : targetProcessIds) {
            List<FlowRecord> records = recordsByProcess.get(processId);
            long oldRuntimeId = records.get(0).getWorkRuntimeId();
            // 先解除与旧快照的合并待办关联，此时记录的合并键仍指向旧快照
            flowRecordService.detachTodoMergeRecords(records);
            records.forEach(record -> record.setWorkRuntimeId(newRuntimeId));
            // 保存记录的同时按新的合并键重建待办与合并关系
            repositoryHolder.saveRecords(records);

            FlowEventPublisher.push(new FlowRecordForceSyncEvent(records.get(0), oldRuntimeId, newRuntimeId, mock));
        }

        return new FlowForceSyncResult(newRuntimeId, latestRuntime.getWorkVersion(), targetProcessIds, skippedProcessIds);
    }

    /**
     * 加载并校验选中的流程实例：必须存在、处于运行中，且实例内记录指向同一份运行时快照。
     */
    private Map<String, List<FlowRecord>> loadRunningInstances() {
        Map<String, List<FlowRecord>> recordsByProcess = new LinkedHashMap<>();
        for (String processId : request.getProcessIds()) {
            if (recordsByProcess.containsKey(processId)) {
                continue;
            }
            List<FlowRecord> records = flowRecordService.findFlowRecordByProcessId(processId);
            if (records == null || records.isEmpty()) {
                throw FlowNotFoundException.process(processId);
            }
            long runtimeId = records.get(0).getWorkRuntimeId();
            for (FlowRecord record : records) {
                if (record.getFlowState() != FlowRecord.SATE_FLOW_RUNNING) {
                    throw FlowStateException.forceSyncInstanceNotRunning(processId);
                }
                if (record.getWorkRuntimeId() != runtimeId) {
                    throw FlowStateException.forceSyncRuntimeInconsistent(processId);
                }
            }
            recordsByProcess.put(processId, records);
        }
        return recordsByProcess;
    }
}
