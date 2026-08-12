package com.codingapi.flow.mock.repository;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowRecordRepositoryMockImpl implements FlowRecordRepository {

    private final Map<Long, FlowRecord> cache = new HashMap<>();
    private long nextId = 1;

    @Override
    public FlowRecord get(long id) {
        return cache.get(id);
    }

    public List<FlowRecord> findAll(){
        return this.cache.values().stream()
                .filter(item->!item.isRevoked())
                .toList();
    }

    @Override
    public List<FlowRecord> findByIds(List<Long> ids) {
        return ids.stream().map(cache::get).toList();
    }

    public List<FlowRecord> findTodoByOperator(long operatorId) {
        return cache.values().stream().filter(flowRecord -> flowRecord.getCurrentOperatorId() == operatorId && flowRecord.isTodo()).toList();
    }

    public List<FlowRecord> findDoneByOperator(long operatorId) {
        return cache.values().stream().filter(flowRecord -> flowRecord.getCurrentOperatorId() == operatorId && !flowRecord.isTodo()).toList();
    }

    public List<FlowRecord> findNotifyByOperator(long operatorId) {
        return cache.values().stream().filter(flowRecord -> flowRecord.getCurrentOperatorId() == operatorId && flowRecord.isNotify()).toList();
    }


    @Override
    public void save(FlowRecord flowRecord) {
        if (flowRecord.getId() > 0) {
            cache.put(flowRecord.getId(), flowRecord);
        } else {
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他记录（mock 模式下 generateRecordId() 返回 0，id 由仓储分配）
            flowRecord.setId(nextId++);
            cache.put(flowRecord.getId(), flowRecord);
        }
    }

    @Override
    public void saveAll(List<FlowRecord> flowRecords) {
        for (FlowRecord flowRecord : flowRecords) {
            this.save(flowRecord);
        }
    }

    @Override
    public void delete(FlowRecord flowRecord) {
        cache.remove(flowRecord.getId());
    }

    @Override
    public List<FlowRecord> findCurrentNodeRecords(long fromId, String nodeId) {
        return cache.values().stream().filter(flowRecord ->
                        flowRecord.getFromId() == fromId
                                && flowRecord.getNodeId().equals(nodeId)
                                && !flowRecord.isRevoked()
                )
                .toList();
    }

    @Override
    public List<FlowRecord> findProcessRecords(String processId) {
        return cache.values().stream().filter(flowRecord ->
                flowRecord.getProcessId().equals(processId)
                        && !flowRecord.isRevoked()
                        && !flowRecord.isHidden()
        ).toList();
    }

    @Override
    public List<FlowRecord> findTodoRecords(String processId) {
        return cache.values().stream().filter(flowRecord ->
                        flowRecord.getProcessId().equals(processId)
                                && flowRecord.isTodo()
                )
                .toList();
    }

    @Override
    public List<FlowRecord> findAfterRecords(String processId, long fromId) {
        return cache.values().stream().filter(flowRecord ->
                flowRecord.getProcessId().equals(processId)
                        && flowRecord.getFromId() >= fromId
                        && !flowRecord.isRevoked()
                        && !flowRecord.isHidden()
        ).toList();
    }

    @Override
    public List<FlowRecord> findBeforeRecords(String processId, long id) {
        return cache.values().stream().filter(flowRecord ->
                flowRecord.getProcessId().equals(processId)
                        && flowRecord.getId() < id
                        && !flowRecord.isRevoked()
                        && !flowRecord.isHidden()
        ).toList();
    }
}
