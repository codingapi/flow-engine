package com.codingapi.flow.transfer.strategy;

import com.codingapi.flow.transfer.WorkflowTransferData;

/**
 * 流程导出Schema生成策略。
 */
public interface WorkflowExportStrategy {

    String format();

    int schemaVersion();

    String serialize(WorkflowTransferData data);
}
