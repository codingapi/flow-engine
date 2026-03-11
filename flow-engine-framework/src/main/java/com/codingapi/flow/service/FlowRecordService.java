package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class FlowRecordService {

    private final FlowTodoRecordRepository flowTodoRecordRepository;
    private final FlowTodoMergeRepository flowTodoMergeRepository;
    private final FlowRecordRepository flowRecordRepository;


    protected void saveAll(List<FlowRecord> flowRecords){
        this.flowRecordRepository.saveAll(flowRecords);
    }

    public void saveFlowRecords(List<FlowRecord> flowRecords){
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecords);
        flowRecordSaveService.saveAll();
    }

    public void saveFlowRecord(FlowRecord flowRecord) {
        FlowRecordSaveService flowRecordSaveService = new FlowRecordSaveService(flowRecord);
        flowRecordSaveService.saveAll();
    }

    public void saveFlowTodos(List<FlowTodoRecord> todoList) {
        flowTodoRecordRepository.saveAll(todoList);
    }

    public void saveFlowMerges(List<FlowTodoMerge> mergeList) {
        this.flowTodoMergeRepository.saveAll(mergeList);
    }

    public FlowRecord getFlowRecord(long id){
        return flowRecordRepository.get(id);
    }

    public FlowTodoRecord getFlowTodoByMergeKey(String mergeKey) {
        return flowTodoRecordRepository.getByMergeKey(mergeKey);
    }

    public List<FlowTodoMerge> findFlowTodoMergeByTodoId(long mergeId) {
        return flowTodoMergeRepository.findByTodoId(mergeId);
    }

    public List<FlowRecord> findFlowRecordByIds(List<Long> list) {
        return flowRecordRepository.findByIds(list);
    }

    public List<FlowRecord> findFlowRecordBeforeRecords(String processId, long recordId) {
        return flowRecordRepository.findBeforeRecords(processId, recordId);
    }

    public List<FlowRecord> findFlowRecordByProcessId(String processId) {
        return flowRecordRepository.findProcessRecords(processId);
    }


    public List<FlowRecord> findFlowRecordAfterRecords(String processId, long recordId) {
        return flowRecordRepository.findAfterRecords(processId,recordId);
    }

    public List<FlowRecord> findFlowRecordTodoRecords(String processId) {
        return flowRecordRepository.findTodoRecords(processId);
    }


    public void deleteFlowMerge(FlowTodoMerge merge) {
        this.flowTodoMergeRepository.delete(merge);
    }


    public void deleteFlowRecord(FlowRecord flowRecord){
        this.flowRecordRepository.delete(flowRecord);
    }

    public void saveFlowTodo(FlowTodoRecord todoMargeRecord) {
        this.flowTodoRecordRepository.save(todoMargeRecord);
    }

    public void deleteFlowTodo(FlowTodoRecord todoMargeRecord) {
        this.flowTodoRecordRepository.delete(todoMargeRecord);
    }

    public List<FlowRecord> findFlowRecordCurrentNodeRecords(long fromId, String nodeId) {
        return this.flowRecordRepository.findCurrentNodeRecords(fromId,nodeId);
    }



}
