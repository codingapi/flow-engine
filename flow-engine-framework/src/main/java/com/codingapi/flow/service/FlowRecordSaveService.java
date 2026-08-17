package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程记录保存服务,负责保存流程记录和待办记录的合并关系
 */
class FlowRecordSaveService {

    /**
     * 批量加载已存在待办时单批查询的 key 上限，避免单个 IN 子句过大（SQL 限制）与结果集过大（内存）。
     */
    private static final int TODO_KEY_BATCH_SIZE = 500;

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
        // 批量加载已存在的待办（而非逐条 getByTodoKey 的 N+1）：
        // 按分块大小分批 findByKeys，避免海量 key 拼在单个 IN 子句里导致 SQL/结果集过大（OOM 风险）。
        List<String> todoKeys = flowRecords.stream()
                .filter(FlowRecord::isTodo)
                .map(FlowRecord::getTodoKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, FlowTodoRecord> existedByKey = new HashMap<>();
        for (int start = 0; start < todoKeys.size(); start += TODO_KEY_BATCH_SIZE) {
            List<String> chunk = todoKeys.subList(start, Math.min(start + TODO_KEY_BATCH_SIZE, todoKeys.size()));
            for (FlowTodoRecord existed : flowTodoRecordRepository.findByKeys(chunk)) {
                existedByKey.put(existed.getTodoKey(), existed);
            }
        }

        List<FlowTodoRecord> flowTodoRecords = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isTodo()) {
                FlowTodoRecord todoMargeRecord = existedByKey.get(flowRecord.getTodoKey());
                if (todoMargeRecord == null) {
                    todoMargeRecord = new FlowTodoRecord(flowRecord);
                    existedByKey.put(flowRecord.getTodoKey(), todoMargeRecord);
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
                    FlowTodoRecord todoMargeRecord = flowTodoRecordRepository.getByTodoKey(flowRecord.getTodoKey());
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
                    FlowTodoRecord flowTodoRecord = flowTodoRecordRepository.getByTodoKey(flowRecord.getTodoKey());
                    if (flowTodoRecord != null) {
                        flowTodoRecordRepository.delete(flowTodoRecord);
                    }
                }
            }else {
                if(flowRecord.isRevoked()){
                    FlowTodoRecord flowTodoRecord = flowTodoRecordRepository.getByTodoKey(flowRecord.getTodoKey());
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
