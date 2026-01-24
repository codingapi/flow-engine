package com.codingapi.flow.repository;

import com.codingapi.flow.domain.UrgeInterval;

import java.util.HashMap;
import java.util.Map;

public class UrgeIntervalRepositoryImpl implements UrgeIntervalRepository{

    private final Map<Long, UrgeInterval> cache = new HashMap<>();


    @Override
    public UrgeInterval getLatest(String processId, long recordId) {
        return cache.values().stream()
                .filter(urgeInterval -> urgeInterval.getProcessId().equals(processId) && urgeInterval.getRecordId() == recordId).sorted((o1, o2) -> (int)(o2.getCreateTime() - o1.getCreateTime()))
                .findFirst().orElse(null);
    }

    @Override
    public void save(UrgeInterval urgeInterval) {
        if (urgeInterval.getId() > 0) {
            cache.put(urgeInterval.getId(), urgeInterval);
        } else {
            long id = cache.size() + 1;
            urgeInterval.setId(id);
            cache.put(id, urgeInterval);
        }
    }
}
