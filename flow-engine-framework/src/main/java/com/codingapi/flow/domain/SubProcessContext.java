package com.codingapi.flow.domain;

import com.codingapi.flow.record.FlowRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 子流程结果脚本执行上下文。
 */
@Getter
@AllArgsConstructor
public class SubProcessContext {

    /**
     * 本次子流程执行的聚合记录（含全部子流程实例）
     */
    private final SubProcessRecord subProcessRecord;

    /**
     * 当前已结束的子流程最终执行记录（触发结果判定的那条）
     */
    private final FlowRecord currentSubProcessRecord;
}
