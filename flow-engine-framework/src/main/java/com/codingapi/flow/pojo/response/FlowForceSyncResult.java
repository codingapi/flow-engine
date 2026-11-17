package com.codingapi.flow.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运行中流程实例强制同步结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowForceSyncResult {

    /**
     * 本次同步使用的运行时快照id（同一设计版本下多个实例共享同一份快照）
     */
    private long workflowRuntimeId;

    /**
     * 该快照对应的设计版本（流程设计的更新时间）
     */
    private long workVersion;

    /**
     * 已换绑到新快照的流程实例
     */
    private List<String> syncedProcessIds;

    /**
     * 已是最新快照而跳过的流程实例
     */
    private List<String> skippedProcessIds;
}
