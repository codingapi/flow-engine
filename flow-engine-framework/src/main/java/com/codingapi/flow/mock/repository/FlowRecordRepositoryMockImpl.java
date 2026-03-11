package com.codingapi.flow.mock.repository;

import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.repository.FlowRecordRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowRecordRepositoryMockImpl implements FlowRecordRepository {

    private final Map<Long, FlowRecord> cache = new HashMap<>();

    @Override
    public FlowRecord get(long id) {
        return cache.get(id);
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


    @Override
    public void save(FlowRecord flowRecord) {
        if (flowRecord.getId() > 0) {
            cache.put(flowRecord.getId(), flowRecord);
        } else {
            long id = cache.size() + 1;
            flowRecord.setId(id);
            cache.put(id, flowRecord);
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
                flowRecord.getProcessId().equals(processId))
                .toList();
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
