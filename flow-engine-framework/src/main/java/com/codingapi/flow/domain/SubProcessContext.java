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

    private final SubProcessRecord subProcessRecord;
    private final FlowRecord currentSubProcessRecord;
}
