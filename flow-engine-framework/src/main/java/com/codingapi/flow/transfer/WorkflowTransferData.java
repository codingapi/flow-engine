package com.codingapi.flow.transfer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 与Schema版本无关的流程迁移数据。
 */
@Getter
@AllArgsConstructor
public class WorkflowTransferData {

    private final Map<String, Object> workflow;

    private final List<Map<String, Object>> versions;

    private final Map<String, String> groovyScripts;
}
