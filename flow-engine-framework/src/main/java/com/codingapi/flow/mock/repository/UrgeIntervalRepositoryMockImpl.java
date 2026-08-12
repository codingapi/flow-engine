package com.codingapi.flow.mock.repository;

import com.codingapi.flow.domain.UrgeInterval;
import com.codingapi.flow.repository.UrgeIntervalRepository;

import java.util.HashMap;
import java.util.Map;

public class UrgeIntervalRepositoryMockImpl implements UrgeIntervalRepository {

    private final Map<Long, UrgeInterval> cache = new HashMap<>();
    private long nextId = 1;


    @Override
    public UrgeInterval getLatest(String processId, long recordId) {
        return cache.values().stream()
                .filter(urgeInterval -> urgeInterval.getProcessId().equals(processId) && urgeInterval.getRecordId() == recordId).sorted((o1, o2) -> (int) (o2.getCreateTime() - o1.getCreateTime()))
                .findFirst().orElse(null);
    }

    @Override
    public void save(UrgeInterval urgeInterval) {
        if (urgeInterval.getId() > 0) {
            cache.put(urgeInterval.getId(), urgeInterval);
        } else {
            // 使用单调递增 id：删除后的 cache.size()+1 可能与已删除的 id 重复，
            // 进而覆盖其他记录
            urgeInterval.setId(nextId++);
            cache.put(urgeInterval.getId(), urgeInterval);
        }
    }
}
