package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

        // 幂等登记：预加载已存在待办的合并关系，用于判断某条流程记录是否已登记过，
        // 避免已读/详情等场景重复保存时反复新增合并关系、膨胀合并计数（issue #223）
        List<Long> existedTodoIds = existedByKey.values().stream()
                .map(FlowTodoRecord::getId)
                .toList();
        Map<Long, Set<Long>> recordIdsByTodoId = new HashMap<>();
        if (!existedTodoIds.isEmpty()) {
            for (FlowTodoMerge relation : flowTodoMergeRepository.findByTodoIds(existedTodoIds)) {
                recordIdsByTodoId.computeIfAbsent(relation.getTodoId(), k -> new HashSet<>())
                        .add(relation.getRecordId());
            }
        }

        // 需要新增的合并关系：todoKey -> 流程记录id（新建待办或该流程记录首次登记时产生）
        Map<String, List<Long>> relationCandidates = new HashMap<>();

        List<FlowTodoRecord> flowTodoRecords = new ArrayList<>();
        for (FlowRecord flowRecord : flowRecords) {
            if (flowRecord.isTodo()) {
                FlowTodoRecord todoMargeRecord = existedByKey.get(flowRecord.getTodoKey());
                if (todoMargeRecord == null) {
                    todoMargeRecord = new FlowTodoRecord(flowRecord);
                    existedByKey.put(flowRecord.getTodoKey(), todoMargeRecord);
                    if (flowRecord.isMergeable()) {
                        relationCandidates
                                .computeIfAbsent(flowRecord.getTodoKey(), k -> new ArrayList<>())
                                .add(flowRecord.getId());
                    }
                } else {
                    todoMargeRecord.update(flowRecord);
                    if (flowRecord.isMergeable()
                            && !recordIdsByTodoId.getOrDefault(todoMargeRecord.getId(), Set.of())
                                    .contains(flowRecord.getId())) {
                        todoMargeRecord.addMergeCount();
                        relationCandidates
                                .computeIfAbsent(flowRecord.getTodoKey(), k -> new ArrayList<>())
                                .add(flowRecord.getId());
                    }
                }
                flowTodoRecords.add(todoMargeRecord);
            }
        }
        if (!flowTodoRecords.isEmpty()) {
            flowTodoRecordRepository.saveAll(flowTodoRecords);
        }

        if (!relationCandidates.isEmpty()) {
            List<FlowTodoMerge> relationList = new ArrayList<>();
            for (Map.Entry<String, List<Long>> entry : relationCandidates.entrySet()) {
                FlowTodoRecord todoMargeRecord = existedByKey.get(entry.getKey());
                for (long recordId : entry.getValue()) {
                    relationList.add(new FlowTodoMerge(0L, todoMargeRecord.getId(), recordId,
                            todoMargeRecord.getCreateTime()));
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
