package com.codingapi.flow.transfer.strategy;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.transfer.WorkflowTransferData;

/**
 * 流程导入Schema解析策略。
 */
public interface WorkflowImportStrategy {

    String format();

    int schemaVersion();

    WorkflowTransferData deserialize(JSONObject data);
}
