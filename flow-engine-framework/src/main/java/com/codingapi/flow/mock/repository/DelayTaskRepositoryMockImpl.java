package com.codingapi.flow.mock.repository;

import com.codingapi.flow.domain.DelayTask;
import com.codingapi.flow.repository.DelayTaskRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelayTaskRepositoryMockImpl implements DelayTaskRepository {

    private final Map<String, DelayTask> cache = new HashMap<>();

    @Override
    public void save(DelayTask task) {
        cache.put(task.getId(), task);
    }

    @Override
    public void delete(DelayTask delayTask) {
        cache.remove(delayTask.getId());
    }

    @Override
    public List<DelayTask> findAll() {
        return cache.values().stream().toList();
    }
}
