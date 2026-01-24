package com.codingapi.flow.repository;

import com.codingapi.flow.domain.DelayTask;

import java.util.List;

/**
 * 延时任务仓库
 */
public interface DelayTaskRepository {

    void save(DelayTask task);

    void delete(DelayTask delayTask);

    List<DelayTask> findAll();

}
