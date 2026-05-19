package com.codingapi.flow.cache;

import com.codingapi.flow.workflow.runtime.WorkflowRuntime;
import lombok.Getter;

import java.util.function.Supplier;

public class WorkflowRuntimeCache {

    public static final int MAX_CACHE_SZE = 1024;

    private final LinkedHashCache<Long, WorkflowRuntime> cache;

    private WorkflowRuntimeCache() {
        this.cache = new LinkedHashCache<>(MAX_CACHE_SZE);
    }

    @Getter
    private final static WorkflowRuntimeCache instance = new WorkflowRuntimeCache();


    public void sync(WorkflowRuntime workflowRuntime) {
        this.cache.put(workflowRuntime.getId(), workflowRuntime);
    }

    public WorkflowRuntime get(long runtimeId, Supplier<WorkflowRuntime> defaultLoader) {
        WorkflowRuntime current = this.cache.get(runtimeId);

        if (current == null) {
            current = defaultLoader.get();
            if (current != null) {
                this.sync(current);
            }
        }

        return current;
    }


}
