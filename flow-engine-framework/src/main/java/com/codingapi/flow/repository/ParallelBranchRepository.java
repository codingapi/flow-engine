package com.codingapi.flow.repository;

/**
 * 并行分支控制仓库
 */
public interface ParallelBranchRepository {

    int getTriggerCount(String parallelId);

    void addTriggerCount(String parallelId);

    void clearTriggerCount(String parallelId);

}
