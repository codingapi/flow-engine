package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.List;

/**
 *  流程记录服务对象
 */
public class FlowRecordService {

    private final FlowTodoRecordRepository flowTodoRecordRepository;
    private final FlowTodoMergeRepository flowTodoMergeRepository;
    private final FlowRecordRepository flowRecordRepository;

    /**
     * 是否为 Mock 模式，用于标记保存事件的 mock 属性
     */
    private final boolean mock;

    public FlowRecordService(FlowTodoRecordRepository flowTodoRecordRepository,
                             FlowTodoMergeRepository flowTodoMergeRepository,
                             FlowRecordRepository flowRecordRepository) {
        this(flowTodoRecordRepository, flowTodoMergeRepository, flowRecordRepository, false);
    }

    public FlowRecordService(FlowTodoRecordRepository flowTodoRecordRepository,
                             FlowTodoMergeRepository flowTodoMergeRepository,
                             FlowRecordRepository flowRecordRepository,
                             boolean mock) {
        this.flowTodoRecordRepository = flowTodoRecordRepository;
        this.flowTodoMergeRepository = flowTodoMergeRepository;
        this.flowRecordRepository = flowRecordRepository;
        this.mock = mock;
    }


    /**
     * 保存流程记录
     * @param flowRecords 流程记录列表
     */
    public void saveFlowRecords(List<FlowRecord> flowRecords){
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecords, this.mock);
        flowRecordSaveService.registerRepositories(this.flowTodoRecordRepository,this.flowTodoMergeRepository,this.flowRecordRepository);
        flowRecordSaveService.saveAll();
    }

    /**
     * 保存流程记录
     * @param flowRecord 流程记录
     */
    public void saveFlowRecord(FlowRecord flowRecord) {
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecord, this.mock);
        flowRecordSaveService.registerRepositories(this.flowTodoRecordRepository,this.flowTodoMergeRepository,this.flowRecordRepository);
        flowRecordSaveService.saveAll();
    }


    /**
     * 从待办合并组中摘除指定流程记录（issue #281）。
     *
     * <p>运行实例换绑运行时快照前，需要先解除其记录与旧快照合并待办的关联，
     * 换绑后重新保存记录即可按新的合并键重建待办组。</p>
     *
     * @param flowRecords 待摘除的流程记录（须仍指向旧快照的合并键）
     */
    public void detachTodoMergeRecords(List<FlowRecord> flowRecords) {
        FlowTodoMigrateService flowTodoMigrateService = new FlowTodoMigrateService(flowRecords);
        flowTodoMigrateService.registerRepositories(this.flowTodoRecordRepository, this.flowTodoMergeRepository, this.flowRecordRepository);
        flowTodoMigrateService.detach();
    }

    /**
     * 获取流程的合并记录
     * @param mergeKey 流程合并key
     * @return 流程合并记录
     */
    public List<FlowRecord> getMergeRecord(String mergeKey){
        FlowTodoRecord todoRecord = flowTodoRecordRepository.getByTodoKey(mergeKey);
        List<FlowTodoMerge> todoMerges = flowTodoMergeRepository.findByTodoId(todoRecord.getId());
        return this.findFlowRecordByIds(todoMerges.stream().map(FlowTodoMerge::getRecordId).toList());
    }


    /**
     * 获取流程记录
     * @param id 流程id
     * @return 流程记录
     */
    public FlowRecord getFlowRecord(long id){
        return flowRecordRepository.get(id);
    }

    /**
     * 批量查询流程记录
     * @param list 流程记录ID
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordByIds(List<Long> list) {
        return flowRecordRepository.findByIds(list);
    }

    /**
     * 查询之前的流程的记录
     * @param processId 流程惟一标识
     * @param recordId 开始记录
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordBeforeRecords(String processId, long recordId) {
        return flowRecordRepository.findBeforeRecords(processId, recordId);
    }

    /**
     * 查询当前流程的记录
     * @param processId 流程唯一标识
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordByProcessId(String processId) {
        return flowRecordRepository.findProcessRecords(processId);
    }

    /**
     * 查询流程之后的记录
     * @param processId 流程唯一标识
     * @param recordId 开始记录
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordAfterRecords(String processId, long recordId) {
        return flowRecordRepository.findAfterRecords(processId,recordId);
    }

    /**
     * 查询流程下的所有待办记录
     * @param processId 流程记录
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordTodoRecords(String processId) {
        return flowRecordRepository.findTodoRecords(processId);
    }

    /**
     * 查询当前节点的流程记录
     * @param fromId 上级id
     * @param nodeId 当前节点id
     * @return 流程记录
     */
    public List<FlowRecord> findFlowRecordCurrentNodeRecords(long fromId, String nodeId) {
        return this.flowRecordRepository.findCurrentNodeRecords(fromId,nodeId);
    }


    /**
     * 删除流程记录及其关联的待办数据
     * <p>
     * 仅用于作废尚未流转的流程实例,会同时清理该流程记录对应的待办记录与合并关系
     * @param flowRecord 流程记录
     */
    public void deleteFlowRecord(FlowRecord flowRecord) {
        FlowTodoRecord flowTodoRecord = flowTodoRecordRepository.getByTodoKey(flowRecord.getTodoKey());
        if (flowTodoRecord != null) {
            // 清理待办合并关系
            List<FlowTodoMerge> todoMerges = flowTodoMergeRepository.findByTodoId(flowTodoRecord.getId());
            if (todoMerges != null) {
                for (FlowTodoMerge todoMerge : todoMerges) {
                    flowTodoMergeRepository.delete(todoMerge);
                }
            }
            // 清理待办记录
            flowTodoRecordRepository.delete(flowTodoRecord);
        }
        // 删除流程记录
        flowRecordRepository.delete(flowRecord);
    }

}
