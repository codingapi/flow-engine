package com.codingapi.flow.repository;

import com.codingapi.flow.record.FlowRecord;

import java.util.List;

/**
 * 模拟生产 JPA 仓储读写语义的 copy-on-read 流程记录仓储。
 * <p>
 * 生产环境的 {@code FlowRecordRepositoryImpl} 每次读写都会在 FlowRecord 与 JPA 实体之间转换，
 * 因此「保存」只落库当前快照，「读取」总是返回基于最近一次保存状态的新对象，
 * 内存中未保存的修改不会对其他读取方可见。
 * <p>
 * 该包装器用于在内存环境下复现对象隔离问题（issue #184）：
 * 子流程自动完成后在嵌套执行中把主流程触发记录标记为完成，但外层流程动作随后
 * 以陈旧的运行中状态再次保存该记录，导致最终流程状态被覆盖回「运行中」。
 */
public class CopyOnReadFlowRecordRepository implements FlowRecordRepository {

    private final FlowRecordRepositoryImpl delegate;

    public CopyOnReadFlowRecordRepository() {
        this.delegate = new FlowRecordRepositoryImpl();
    }

    @Override
    public FlowRecord get(long id) {
        FlowRecord record = delegate.get(id);
        return record == null ? null : copy(record);
    }

    @Override
    public List<FlowRecord> findByIds(List<Long> ids) {
        return delegate.findByIds(ids).stream().map(CopyOnReadFlowRecordRepository::copy).toList();
    }

    @Override
    public void save(FlowRecord flowRecord) {
        // 与生产 JPA 仓储一致：id 由存储层分配并回写到原对象
        FlowRecord snapshot = copy(flowRecord);
        delegate.save(snapshot);
        flowRecord.setId(snapshot.getId());
    }

    @Override
    public void saveAll(List<FlowRecord> flowRecords) {
        for (FlowRecord flowRecord : flowRecords) {
            this.save(flowRecord);
        }
    }

    @Override
    public void delete(FlowRecord flowRecord) {
        delegate.delete(flowRecord);
    }

    @Override
    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return delegate.findCurrentNodeRecords(fromId, nodeId).stream()
                .map(CopyOnReadFlowRecordRepository::copy)
                .toList();
    }

    @Override
    public List<FlowRecord> findProcessRecords(String processId) {
        return delegate.findProcessRecords(processId).stream()
                .map(CopyOnReadFlowRecordRepository::copy)
                .toList();
    }

    @Override
    public List<FlowRecord> findTodoRecords(String processId) {
        return delegate.findTodoRecords(processId).stream()
                .map(CopyOnReadFlowRecordRepository::copy)
                .toList();
    }

    @Override
    public List<FlowRecord> findAfterRecords(String processId, long fromId) {
        return delegate.findAfterRecords(processId, fromId).stream()
                .map(CopyOnReadFlowRecordRepository::copy)
                .toList();
    }

    @Override
    public List<FlowRecord> findBeforeRecords(String processId, long id) {
        return delegate.findBeforeRecords(processId, id).stream()
                .map(CopyOnReadFlowRecordRepository::copy)
                .toList();
    }

    /**
     * 按 {@link FlowRecord#FlowRecord(long, long, String, String, String, String, String, long, long,
     * Map, String, long, String, String, String, String, String, String, long, String, long, String,
     * long, String, String, int, boolean, boolean, boolean, int, int, long, long, long, boolean,
     * long, String, String, long, boolean, long, String, long, String, String, int)} 构造顺序复制记录。
     */
    private static FlowRecord copy(FlowRecord record) {
        return new FlowRecord(record.getId(),
                record.getWorkRuntimeId(),
                record.getWorkTitle(),
                record.getWorkCode(),
                record.getNodeId(),
                record.getNodeType(),
                record.getNodeName(),
                record.getFromId(),
                record.getParentId(),
                record.getFormData(),
                record.getTitle(),
                record.getReadTime(),
                record.getProcessId(),
                record.getActionId(),
                record.getActionType(),
                record.getActionName(),
                record.getAdvice(),
                record.getSignKey(),
                record.getCurrentOperatorId(),
                record.getCurrentOperatorName(),
                record.getSubmitOperatorId(),
                record.getSubmitOperatorName(),
                record.getForwardOperatorId(),
                record.getForwardOperatorName(),
                record.getReturnNodeId(),
                record.getNodeOrder(),
                record.isHidden(),
                record.isRevoked(),
                record.isNotify(),
                record.getRecordState(),
                record.getFlowState(),
                record.getUpdateTime(),
                record.getCreateTime(),
                record.getFinishTime(),
                record.isReadable(),
                record.getCreateOperatorId(),
                record.getCreateOperatorName(),
                record.getErrMessage(),
                record.getTimeoutTime(),
                record.isMergeable(),
                record.getMergeType(),
                record.getInterferedOperatorId(),
                record.getInterferedOperatorName(),
                record.getDelegateId(),
                record.getParallelId(),
                record.getParallelBranchNodeId(),
                record.getParallelBranchTotal());
    }
}