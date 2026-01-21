package com.codingapi.flow.repository;

public interface ParallelBranchRepository {

    int getTriggerCount(String parallelId);

    void addTriggerCount(String parallelId);

    void clearTriggerCount(String parallelId);

}
