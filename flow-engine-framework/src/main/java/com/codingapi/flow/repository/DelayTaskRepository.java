package com.codingapi.flow.repository;

import com.codingapi.flow.delay.DelayTask;

import java.util.List;

public interface DelayTaskRepository {

    void save(DelayTask task);

    void delete(DelayTask delayTask);

    List<DelayTask> findAll();

}
