package com.codingapi.flow.transfer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.codingapi.flow.exception.FlowNotFoundException;
import com.codingapi.flow.exception.WorkflowTransferException;
import com.codingapi.flow.generator.FlowIDGeneratorGatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.repository.WorkflowVersionRepository;
import com.codingapi.flow.service.WorkflowGroovyScriptUtils;
import com.codingapi.flow.transfer.strategy.WorkflowExportStrategy;
import com.codingapi.flow.transfer.strategy.WorkflowImportStrategy;
import com.codingapi.flow.transfer.strategy.WorkflowSchemaStrategyRegistry;
import com.codingapi.flow.transfer.strategy.WorkflowV1SchemaStrategy;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程导入导出的公共业务编排，不感知具体Schema字段差异。
 */
public class WorkflowTransferService {

    private static final List<String> VERSION_DEFINITION_FIELDS = List.of(
            "form", "operatorCreateScript", "nodes", "strategies", "maxNestDepth");

    private final WorkflowVersionRepository workflowVersionRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowSchemaStrategyRegistry strategyRegistry;
    private final WorkflowGroovyScriptTransferService scriptTransferService;

    public WorkflowTransferService(WorkflowVersionRepository workflowVersionRepository,
                                   WorkflowRepository workflowRepository) {
        this.workflowVersionRepository = workflowVersionRepository;
        this.workflowRepository = workflowRepository;
        this.strategyRegistry = new WorkflowSchemaStrategyRegistry();
        this.scriptTransferService = new WorkflowGroovyScriptTransferService();
    }

    /**
     * 按最新Schema导出流程及其全部版本。
     */
    public String exportWorkflow(String workId) {
        Workflow workflow = workflowRepository.getById(workId);
        if (workflow == null) {
            throw FlowNotFoundException.workflow(workId);
        }

        List<WorkflowVersion> versions = new ArrayList<>(workflowVersionRepository.findVersion(workId));
        if (versions.isEmpty()) {
            versions.add(new WorkflowVersion(workflow));
        }
        versions.sort(Comparator.comparingLong(WorkflowVersion::getUpdatedTime)
                .thenComparingLong(WorkflowVersion::getId));

        List<WorkflowVersion> currentVersions = versions.stream()
                .filter(WorkflowVersion::isCurrent)
                .toList();
        if (currentVersions.size() != 1) {
            throw WorkflowTransferException.invalidSchema(
                    String.format("Workflow %s must have exactly one current version", workId));
        }
        WorkflowVersion currentVersion = currentVersions.get(0);

        Map<String, Object> workflowData = workflowSummary(workflow, currentVersion.getVersionName());
        List<Map<String, Object>> versionData = versions.stream().map(this::versionData).toList();

        Map<String, String> scripts = scriptTransferService.exportScripts(versions);
        WorkflowTransferData transferData = new WorkflowTransferData(workflowData, versionData, scripts);
        WorkflowExportStrategy exportStrategy = strategyRegistry.resolveExport(
                WorkflowV1SchemaStrategy.FORMAT,
                WorkflowV1SchemaStrategy.SCHEMA_VERSION);
        return exportStrategy.serialize(transferData);
    }

    /**
     * 根据文件Schema与导入模式导入流程。
     */
    public String importWorkflow(String json, IFlowOperator operator, WorkflowImportMode mode) {
        if (json == null || json.isBlank()) {
            throw WorkflowTransferException.invalidSchema("Import content cannot be empty");
        }
        JSONObject root;
        try {
            root = JSON.parseObject(json);
        } catch (RuntimeException exception) {
            throw new WorkflowTransferException(
                    "workflow.transfer.schema.parseError", "Invalid workflow JSON", exception);
        }
        if (root == null) {
            throw WorkflowTransferException.invalidSchema("Import content must be a JSON object");
        }
        WorkflowImportStrategy importStrategy = strategyRegistry.resolveImport(root);
        WorkflowTransferData transferData = importStrategy.deserialize(root);
        return persistImport(transferData, operator,
                mode == null ? WorkflowImportMode.INCREMENTAL : mode);
    }

    private String persistImport(WorkflowTransferData transferData,
                                 IFlowOperator operator,
                                 WorkflowImportMode mode) {
        if (operator == null) {
            throw WorkflowTransferException.invalidSchema("Import operator cannot be null");
        }
        String sourceCode = stringValue(transferData.getWorkflow().get("code"));
        if (sourceCode == null || sourceCode.isBlank()) {
            throw WorkflowTransferException.invalidSchema("workflow.code is required");
        }

        TargetIdentity identity = resolveIdentity(sourceCode, mode);
        long importTime = identity.updatedTime();
        boolean currentEnable = booleanValue(transferData.getWorkflow().get("enable"), false);
        List<WorkflowVersion> importedVersions = new ArrayList<>();
        for (Map<String, Object> versionData : transferData.getVersions()) {
            String versionCode = stringValue(versionData.get("code"));
            if (!sourceCode.equals(versionCode)) {
                throw WorkflowTransferException.invalidSchema(
                        "versions.code must match workflow.code");
            }
            importedVersions.add(toWorkflowVersion(
                    versionData,
                    identity,
                    operator,
                    importTime,
                    currentEnable));
        }

        List<WorkflowVersion> currentVersions = importedVersions.stream()
                .filter(WorkflowVersion::isCurrent)
                .toList();
        if (currentVersions.size() != 1) {
            throw WorkflowTransferException.invalidSchema(
                    "versions must contain exactly one current version");
        }
        validateCurrentVersion(transferData, currentVersions.get(0));

        scriptTransferService.rebuildScripts(importedVersions, transferData.getGroovyScripts());

        if (mode == WorkflowImportMode.REPLACE) {
            removeReplacedVersions(identity.workId());
        }
        workflowVersionRepository.saveAll(importedVersions);
        workflowRepository.save(currentVersions.get(0).toWorkflow());
        return identity.workId();
    }

    private TargetIdentity resolveIdentity(String sourceCode, WorkflowImportMode mode) {
        long now = System.currentTimeMillis();
        if (mode == WorkflowImportMode.REPLACE) {
            Workflow target = workflowRepository.getByCode(sourceCode);
            if (target == null) {
                throw WorkflowTransferException.replaceTargetNotFound(sourceCode);
            }
            return new TargetIdentity(
                    target.getId(),
                    target.getCode(),
                    target.getCreatedTime(),
                    Math.max(now, target.getUpdatedTime() + 1));
        }
        String importedWorkCode = workflowRepository.getByCode(sourceCode) == null
                ? sourceCode
                : generateAvailableWorkCode();
        return new TargetIdentity(
                FlowIDGeneratorGatewayContext.getInstance().generateWorkId(),
                importedWorkCode,
                now,
                now);
    }

    private String generateAvailableWorkCode() {
        String workCode;
        do {
            workCode = FlowIDGeneratorGatewayContext.getInstance().generateWorkCode();
        } while (workflowRepository.getByCode(workCode) != null);
        return workCode;
    }

    private WorkflowVersion toWorkflowVersion(Map<String, Object> versionData,
                                              TargetIdentity identity,
                                              IFlowOperator operator,
                                              long importTime,
                                              boolean currentEnable) {
        Map<String, Object> parsableData = new LinkedHashMap<>(versionData);
        parsableData.put("createdOperator", String.valueOf(operator.getUserId()));
        Workflow sourceWorkflow;
        try {
            sourceWorkflow = Workflow.formJson(JSON.toJSONString(parsableData));
        } catch (RuntimeException exception) {
            throw new WorkflowTransferException(
                    "workflow.transfer.version.parseError",
                    String.format("Invalid workflow version: %s", versionData.get("versionName")),
                    exception);
        }

        boolean current = booleanValue(versionData.get("current"), false);
        boolean enable = current
                ? currentEnable
                : booleanValue(versionData.get("enable"), false);
        long updatedTime = current ? importTime : sourceWorkflow.getUpdatedTime();
        return new WorkflowVersion(
                0,
                stringValue(versionData.get("versionName")),
                current,
                identity.workId(),
                identity.workCode(),
                sourceWorkflow.getTitle(),
                sourceWorkflow.getDescription(),
                operator,
                identity.createdTime(),
                updatedTime,
                sourceWorkflow.getForm(),
                sourceWorkflow.getOperatorCreateScript(),
                sourceWorkflow.getNodes(),
                sourceWorkflow.getStrategies(),
                enable,
                sourceWorkflow.getMaxNestDepth());
    }

    private void validateCurrentVersion(WorkflowTransferData transferData, WorkflowVersion currentVersion) {
        Object expectedCurrentVersion = transferData.getWorkflow().get("currentVersion");
        if (expectedCurrentVersion != null
                && !expectedCurrentVersion.toString().equals(currentVersion.getVersionName())) {
            throw WorkflowTransferException.invalidSchema(
                    "workflow.currentVersion does not match versions.current");
        }
    }

    private void removeReplacedVersions(String workId) {
        List<WorkflowVersion> existingVersions = workflowVersionRepository.findVersion(workId);
        existingVersions.forEach(WorkflowGroovyScriptUtils::deleteScripts);
        // 同时清理主流程脚本，兼容历史数据中主流程与版本脚本key不一致的情况。
        WorkflowGroovyScriptUtils.deleteScripts(workflowRepository.getById(workId));
        workflowVersionRepository.delete(workId);
    }

    private Map<String, Object> workflowSummary(Workflow workflow, String currentVersion) {
        Map<String, Object> data = workflowMap(workflow);
        VERSION_DEFINITION_FIELDS.forEach(data::remove);
        data.put("currentVersion", currentVersion);
        data.put("enable", workflow.isEnable());
        return data;
    }

    private Map<String, Object> versionData(WorkflowVersion version) {
        Map<String, Object> data = workflowMap(version.toWorkflow());
        data.put("versionName", version.getVersionName());
        data.put("current", version.isCurrent());
        data.put("enable", version.isEnable());
        return data;
    }

    private Map<String, Object> workflowMap(Workflow workflow) {
        return new LinkedHashMap<>(JSON.parseObject(workflow.toJson()));
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
    }

    private record TargetIdentity(String workId, String workCode, long createdTime, long updatedTime) {
    }
}
