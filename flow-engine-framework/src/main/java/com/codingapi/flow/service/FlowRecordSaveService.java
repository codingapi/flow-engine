package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程记录保存服务,负责保存流程记录和待办记录的合并关系
 */
class FlowRecordSaveService {

    private final List<FlowRecord> flowRecords;

    private FlowTodoRecordRepository flowTodoRecordRepository;
    private FlowTodoMergeRepository flowTodoMergeRepository;
    private FlowRecordRepository flowRecordRepository;


    public FlowRecordSaveService(List<FlowRecord> flowRecords) {
        this.flowRecords = flowRecords;
    }

    public FlowRecordSaveService(FlowRecord flowRecord) {
        this.flowRecords = new ArrayList<>();
        this.flowRecords.add(flowRecord);
    }

    public void registerRepositories(FlowTodoRecordRepository flowTodoRecordRepository,
                                     FlowTodoMergeRepository flowTodoMergeRepository,
                                     FlowRecordRepository flowRecordRepository) {
        this.flowTodoRecordRepository = flowTodoRecordRepository;
        this.flowTodoMergeRepository = flowTodoMergeRepository;
        this.flowRecordRepository = flowRecordRepository;
    }


    private void saveTodoMargeRecords() {
        List<FlowTodoRecord> flowTodoRecords = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isTodo()) {
                FlowTodoRecord todoMargeRecord = flowTodoRecordRepository.getByMergeKey(flowRecord.getMergeKey());
                if (todoMargeRecord == null) {
                    todoMargeRecord = new FlowTodoRecord(flowRecord);
                } else {
                    todoMargeRecord.update(flowRecord);
                    if (flowRecord.isMergeable()) {
                        todoMargeRecord.addMergeCount();
                    }
                }
                flowTodoRecords.add(todoMargeRecord);
            }
        }
        if (!flowTodoRecords.isEmpty()) {
            flowTodoRecordRepository.saveAll(flowTodoRecords);
        }

        if (!flowTodoRecords.isEmpty()) {
            List<FlowTodoMerge> relationList = new ArrayList<>();
            for (FlowTodoRecord margeRecord : flowTodoRecords) {
                if (margeRecord.isMergeable()) {
                    relationList.add(new FlowTodoMerge(margeRecord));
                }
            }
            flowTodoMergeRepository.saveAll(relationList);
        }
    }

    private void saveRecords() {
        if (!flowRecords.isEmpty()) {
            // 只保存非结束节点的记录,结束节点的记录由流程引擎自动生成,不允许外部修改
            List<FlowRecord> flowRecordList = flowRecords.stream().filter(FlowRecord::isNotEndNode).toList();
            flowRecordRepository.saveAll(flowRecordList);
        }
    }

    private void removeTodoMergeRecords() {
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isDone()) {
                if (flowRecord.isMergeable()) {
                    FlowTodoRecord todoMargeRecord = flowTodoRecordRepository.getByMergeKey(flowRecord.getMergeKey());
                    if (todoMargeRecord != null) {
                        List<FlowTodoMerge> margeRelations = flowTodoMergeRepository.findByTodoId(todoMargeRecord.getId());
                        if (margeRelations != null && !margeRelations.isEmpty()) {
                            for (FlowTodoMerge margeRelation : margeRelations) {
                                if (margeRelation.isRecord(flowRecord.getId())) {
                                    flowTodoMergeRepository.delete(margeRelation);
                                    todoMargeRecord.divMergeCount();
                                    if (todoMargeRecord.hasMergeCount()) {
                                        flowTodoRecordRepository.save(todoMargeRecord);
                                    } else {
                                        flowTodoRecordRepository.delete(todoMargeRecord);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    FlowTodoRecord flowTodoRecord = flowTodoRecordRepository.getByMergeKey(flowRecord.getMergeKey());
                    if (flowTodoRecord != null) {
                        flowTodoRecordRepository.delete(flowTodoRecord);
                    }
                }
            }
        }
    }

    public void saveAll() {
        this.saveRecords();
        this.saveTodoMargeRecords();
        this.removeTodoMergeRecords();
    }
}
