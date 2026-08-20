package com.codingapi.flow.transfer.strategy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.codingapi.flow.exception.WorkflowTransferException;
import com.codingapi.flow.transfer.WorkflowTransferData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V1流程迁移Schema策略。
 */
public class WorkflowV1SchemaStrategy implements WorkflowImportStrategy, WorkflowExportStrategy {

    public static final String FORMAT = "flow-engine-workflow";
    public static final int SCHEMA_VERSION = 1;

    @Override
    public String format() {
        return FORMAT;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public WorkflowTransferData deserialize(JSONObject data) {
        JSONObject workflow = data.getJSONObject("workflow");
        JSONArray versions = data.getJSONArray("versions");
        JSONObject groovyScripts = data.getJSONObject("groovyScripts");
        if (workflow == null) {
            throw WorkflowTransferException.invalidSchema("workflow is required");
        }
        if (versions == null || versions.isEmpty()) {
            throw WorkflowTransferException.invalidSchema("versions cannot be empty");
        }
        if (groovyScripts == null) {
            throw WorkflowTransferException.invalidSchema("groovyScripts is required");
        }

        List<Map<String, Object>> versionList = new ArrayList<>();
        for (Object version : versions) {
            if (!(version instanceof JSONObject versionObject)) {
                throw WorkflowTransferException.invalidSchema("versions must contain JSON objects");
            }
            versionList.add(new LinkedHashMap<>(versionObject));
        }

        Map<String, String> scripts = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : groovyScripts.entrySet()) {
            if (!(entry.getValue() instanceof String content)) {
                throw WorkflowTransferException.invalidSchema(
                        String.format("groovyScripts.%s must be a string", entry.getKey()));
            }
            scripts.put(entry.getKey(), content);
        }
        return new WorkflowTransferData(new LinkedHashMap<>(workflow), versionList, scripts);
    }

    @Override
    public String serialize(WorkflowTransferData data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", format());
        result.put("schemaVersion", schemaVersion());
        result.put("workflow", data.getWorkflow());
        result.put("versions", data.getVersions());
        result.put("groovyScripts", data.getGroovyScripts());
        return JSON.toJSONString(result, SerializerFeature.PrettyFormat);
    }
}
