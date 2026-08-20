package com.codingapi.flow.transfer.strategy;

import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.exception.WorkflowTransferException;

import java.util.HashMap;
import java.util.Map;

/**
 * 按format与schemaVersion匹配导入导出策略。
 */
public class WorkflowSchemaStrategyRegistry {

    private final Map<SchemaKey, WorkflowImportStrategy> importStrategies = new HashMap<>();
    private final Map<SchemaKey, WorkflowExportStrategy> exportStrategies = new HashMap<>();
    private final WorkflowImportStrategy legacyImportStrategy;

    public WorkflowSchemaStrategyRegistry() {
        WorkflowV1SchemaStrategy v1Strategy = new WorkflowV1SchemaStrategy();
        this.register(v1Strategy, v1Strategy);
        this.legacyImportStrategy = new LegacyWorkflowImportStrategy();
    }

    public void register(WorkflowImportStrategy importStrategy, WorkflowExportStrategy exportStrategy) {
        importStrategies.put(new SchemaKey(importStrategy.format(), importStrategy.schemaVersion()), importStrategy);
        exportStrategies.put(new SchemaKey(exportStrategy.format(), exportStrategy.schemaVersion()), exportStrategy);
    }

    public WorkflowImportStrategy resolveImport(JSONObject data) {
        if (!data.containsKey("format") && !data.containsKey("schemaVersion")) {
            return legacyImportStrategy;
        }
        String format = data.getString("format");
        Integer schemaVersion = data.getInteger("schemaVersion");
        if (format == null || schemaVersion == null) {
            throw WorkflowTransferException.invalidSchema("format and schemaVersion must be provided together");
        }
        WorkflowImportStrategy strategy = importStrategies.get(new SchemaKey(format, schemaVersion));
        if (strategy == null) {
            throw WorkflowTransferException.unsupportedSchema(format, schemaVersion);
        }
        return strategy;
    }

    public WorkflowExportStrategy resolveExport(String format, int schemaVersion) {
        WorkflowExportStrategy strategy = exportStrategies.get(new SchemaKey(format, schemaVersion));
        if (strategy == null) {
            throw WorkflowTransferException.unsupportedSchema(format, schemaVersion);
        }
        return strategy;
    }

    private record SchemaKey(String format, int schemaVersion) {
    }
}
