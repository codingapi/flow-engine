package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.ArrayList;
import java.util.Comparator;
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
            // 已办结与已撤销都要把该记录从待办中摘除。
            // 合并待办按「合并依据 + 工作流运行时 + 节点」聚合，组内一条流程记录办结或撤销时，
            // 只能摘除它自己的合并关系，不能连带删除同组其他流程记录的待办（issue #260）
            if (flowRecord.isDone() || flowRecord.isRevoked()) {
                removeTodoRecord(flowRecord);
            }
        }
    }

    private void removeTodoRecord(FlowRecord flowRecord) {
        FlowTodoRecord flowTodoRecord = flowTodoRecordRepository.getByTodoKey(flowRecord.getTodoKey());
        if (flowTodoRecord == null) {
            return;
        }
        if (!flowRecord.isMergeable()) {
            flowTodoRecordRepository.delete(flowTodoRecord);
            return;
        }
        List<FlowTodoMerge> margeRelations = flowTodoMergeRepository.findByTodoId(flowTodoRecord.getId());
        if (margeRelations == null || margeRelations.isEmpty()) {
            return;
        }
        for (FlowTodoMerge margeRelation : margeRelations) {
            if (!margeRelation.isRecord(flowRecord.getId())) {
                continue;
            }
            flowTodoMergeRepository.delete(margeRelation);
            flowTodoRecord.divMergeCount();
            if (flowTodoRecord.hasMergeCount()) {
                // 代表记录可能正是本次办结/撤销的记录，需要重新指向组内剩余的待办记录（issue #240 / #260）
                refreshTodoMergeRecord(flowTodoRecord, margeRelations, flowRecord.getId());
                flowTodoRecordRepository.save(flowTodoRecord);
            } else {
                // 组内已无剩余记录：删除待办行，并清理残余合并关系，避免留下指向已删除待办的孤儿数据
                removeTodoRecordWithRelations(flowTodoRecord);
            }
        }
    }

    /**
     * 删除合并待办行及其全部残余合并关系。
     */
    private void removeTodoRecordWithRelations(FlowTodoRecord flowTodoRecord) {
        List<FlowTodoMerge> margeRelations = flowTodoMergeRepository.findByTodoId(flowTodoRecord.getId());
        if (margeRelations != null) {
            for (FlowTodoMerge margeRelation : margeRelations) {
                flowTodoMergeRepository.delete(margeRelation);
            }
        }
        flowTodoRecordRepository.delete(flowTodoRecord);
    }

    /**
     * 将合并待办的代表记录刷新为组内剩余的待办记录（issue #240）。
     *
     * <p>合并待办的 {@code recordId} 会随组内任意一条记录的保存而更新（{@link FlowTodoRecord#update}），
     * 部分审批后该记录可能恰好是刚办结的记录。此时组内仍有剩余待办，但待办列表行指向已办记录：
     * 列表展示的是已完成的流程，详情也会因代表记录不是待办而无法加载整组。
     * 因此从剩余的合并关系中重新选择一条待办记录回写代表记录。
     *
     * @param todoMargeRecord 合并待办组
     * @param margeRelations  删除合并关系之前的组内关系列表
     * @param doneRecordId    本次办结、且合并关系已被删除的记录id
     */
    private void refreshTodoMergeRecord(FlowTodoRecord todoMargeRecord, List<FlowTodoMerge> margeRelations,
                                        long doneRecordId) {
        List<Long> remainingRecordIds = margeRelations.stream()
                .map(FlowTodoMerge::getRecordId)
                .filter(recordId -> recordId != doneRecordId)
                .distinct()
                .sorted()
                .toList();
        if (remainingRecordIds.isEmpty()) {
            return;
        }
        List<FlowRecord> remainingRecords = flowRecordRepository.findByIds(remainingRecordIds).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(FlowRecord::getId))
                .toList();
        // 代表记录仍是待办时无需变更，避免无谓改变待办列表展示的数据
        boolean representativeTodo = remainingRecords.stream()
                .anyMatch(record -> record.getId() == todoMargeRecord.getRecordId() && record.isTodo());
        if (representativeTodo) {
            return;
        }
        remainingRecords.stream()
                .filter(FlowRecord::isTodo)
                .findFirst()
                .ifPresent(todoMargeRecord::update);
    }

    public void saveAll() {
        this.saveRecords();
        this.saveTodoMargeRecords();
        this.removeTodoMergeRecords();
    }
}
