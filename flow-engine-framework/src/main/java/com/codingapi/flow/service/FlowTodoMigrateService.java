package com.codingapi.flow.service;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.record.FlowTodoMerge;
import com.codingapi.flow.record.FlowTodoRecord;
import com.codingapi.flow.repository.FlowRecordRepository;
import com.codingapi.flow.repository.FlowTodoMergeRepository;
import com.codingapi.flow.repository.FlowTodoRecordRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 待办合并关系迁移服务（issue #281）。
 *
 * <p>合并待办的键为 {@code 合并依据 + workRuntimeId + nodeId}（不含 processId），
 * 同一运行时快照下多个流程实例的待办会合并为一行，因此运行实例换绑快照前必须先把自身记录
 * 从旧合并组中摘除，否则旧组会保留指向已迁移记录的合并关系。</p>
 */
class FlowTodoMigrateService {

    private final List<FlowRecord> flowRecords;

    private FlowTodoRecordRepository flowTodoRecordRepository;
    private FlowTodoMergeRepository flowTodoMergeRepository;
    private FlowRecordRepository flowRecordRepository;

    FlowTodoMigrateService(List<FlowRecord> flowRecords) {
        this.flowRecords = flowRecords == null ? List.of() : flowRecords;
    }

    void registerRepositories(FlowTodoRecordRepository flowTodoRecordRepository,
                              FlowTodoMergeRepository flowTodoMergeRepository,
                              FlowRecordRepository flowRecordRepository) {
        this.flowTodoRecordRepository = flowTodoRecordRepository;
        this.flowTodoMergeRepository = flowTodoMergeRepository;
        this.flowRecordRepository = flowRecordRepository;
    }

    /**
     * 从旧合并待办组中摘除本批流程记录。
     *
     * <p>必须在流程记录的 {@code workRuntimeId} 变更之前调用：此时记录的
     * {@link FlowRecord#getTodoKey()} 仍指向旧快照下的待办组。</p>
     */
    void detach() {
        List<FlowRecord> todoRecords = flowRecords.stream()
                .filter(FlowRecord::isTodo)
                .filter(FlowRecord::isMergeable)
                .toList();
        if (todoRecords.isEmpty()) {
            return;
        }
        Map<String, List<FlowRecord>> recordsByKey = new LinkedHashMap<>();
        for (FlowRecord flowRecord : todoRecords) {
            String todoKey = flowRecord.getTodoKey();
            if (todoKey == null) {
                continue;
            }
            recordsByKey.computeIfAbsent(todoKey, key -> new ArrayList<>()).add(flowRecord);
        }
        for (Map.Entry<String, List<FlowRecord>> entry : recordsByKey.entrySet()) {
            this.detachGroup(entry.getKey(), entry.getValue());
        }
    }

    private void detachGroup(String todoKey, List<FlowRecord> records) {
        FlowTodoRecord todoRecord = flowTodoRecordRepository.getByTodoKey(todoKey);
        if (todoRecord == null) {
            return;
        }
        List<FlowTodoMerge> relations = flowTodoMergeRepository.findByTodoId(todoRecord.getId());
        if (relations == null || relations.isEmpty()) {
            return;
        }

        Set<Long> recordIds = records.stream().map(FlowRecord::getId).collect(Collectors.toSet());
        List<Long> remainedRecordIds = new ArrayList<>();
        int removed = 0;
        for (FlowTodoMerge relation : relations) {
            if (recordIds.contains(relation.getRecordId())) {
                flowTodoMergeRepository.delete(relation);
                removed++;
            } else {
                remainedRecordIds.add(relation.getRecordId());
            }
        }
        if (removed == 0) {
            return;
        }

        for (int i = 0; i < removed; i++) {
            todoRecord.divMergeCount();
        }
        if (!todoRecord.hasMergeCount()) {
            // 组内记录已全部迁移，待办行与残余合并关系一并清理
            flowTodoRecordRepository.delete(todoRecord);
            return;
        }
        if (recordIds.contains(todoRecord.getRecordId())) {
            // 组代表记录被摘除时，重新指向组内剩余的待办记录
            this.refreshRepresentative(todoRecord, remainedRecordIds);
        }
        flowTodoRecordRepository.save(todoRecord);
    }

    private void refreshRepresentative(FlowTodoRecord todoRecord, List<Long> remainedRecordIds) {
        List<Long> sortedRecordIds = remainedRecordIds.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        for (Long recordId : sortedRecordIds) {
            FlowRecord record = flowRecordRepository.get(recordId);
            if (record != null && record.isTodo()) {
                todoRecord.update(record);
                return;
            }
        }
    }
}
