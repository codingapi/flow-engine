package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 *  流程记录服务对象
 */
@AllArgsConstructor
public class FlowRecordService {

    private final FlowTodoRecordRepository flowTodoRecordRepository;
    private final FlowTodoMergeRepository flowTodoMergeRepository;
    private final FlowRecordRepository flowRecordRepository;


    /**
     * 保存流程记录
     * @param flowRecords 流程记录列表
     */
    public void saveFlowRecords(List<FlowRecord> flowRecords){
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecords);
        flowRecordSaveService.registerRepositories(this.flowTodoRecordRepository,this.flowTodoMergeRepository,this.flowRecordRepository);
        flowRecordSaveService.saveAll();
    }

    /**
     * 保存流程记录
     * @param flowRecord 流程记录
     */
    public void saveFlowRecord(FlowRecord flowRecord) {
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecord);
        flowRecordSaveService.registerRepositories(this.flowTodoRecordRepository,this.flowTodoMergeRepository,this.flowRecordRepository);
        flowRecordSaveService.saveAll();
    }


    /**
     * 获取流程的合并记录
     * @param mergeKey 流程合并key
     * @return 流程合并记录
     */
    public List<FlowRecord> getMergeRecord(String mergeKey){
        FlowTodoRecord todoRecord = flowTodoRecordRepository.getByMergeKey(mergeKey);
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

}
