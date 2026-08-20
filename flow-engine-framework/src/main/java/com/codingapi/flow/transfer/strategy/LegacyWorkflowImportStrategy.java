package com.codingapi.flow.transfer.strategy;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.transfer.WorkflowTransferData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 兼容未包含format/schemaVersion的历史单流程文件。
 */
public class LegacyWorkflowImportStrategy implements WorkflowImportStrategy {

    public static final int SCHEMA_VERSION = 0;

    @Override
    public String format() {
        return "legacy-flow-engine-workflow";
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public WorkflowTransferData deserialize(JSONObject data) {
        Map<String, Object> version = new LinkedHashMap<>(data);
        version.put("versionName", null);
        version.put("current", true);
        version.putIfAbsent("enable", false);

        Map<String, Object> workflow = new LinkedHashMap<>(data);
        workflow.put("currentVersion", null);
        workflow.putIfAbsent("enable", false);
        return new WorkflowTransferData(workflow, List.of(version), new LinkedHashMap<>());
    }
}
