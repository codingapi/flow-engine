package com.codingapi.flow.repository;

import java.util.HashMap;
import java.util.Map;

public class ParallelBranchRepositoryImpl implements ParallelBranchRepository {

    private final Map<String, Integer> cache = new HashMap<>();

    @Override
    public int getTriggerCount(String parallelId) {
        Integer value = cache.get(parallelId);
        return value == null ? 0 : value;
    }

    @Override
    public void addTriggerCount(String parallelId) {
        this.cache.put(parallelId, this.getTriggerCount(parallelId) + 1);
    }

    @Override
    public void clearTriggerCount(String parallelId) {
        this.cache.remove(parallelId);
    }
}
