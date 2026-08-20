package com.codingapi.flow.exception;

/**
 * 流程导入导出异常。
 */
public class WorkflowTransferException extends FlowException {

    public WorkflowTransferException(String code, String message) {
        super(code, message);
    }

    public WorkflowTransferException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public static WorkflowTransferException invalidSchema(String message) {
        return new WorkflowTransferException("workflow.transfer.schema.invalid", message);
    }

    public static WorkflowTransferException unsupportedSchema(String format, int schemaVersion) {
        return new WorkflowTransferException(
                "workflow.transfer.schema.unsupported",
                String.format("Unsupported workflow schema: format=%s, schemaVersion=%d", format, schemaVersion));
    }

    public static WorkflowTransferException scriptNotFound(String key) {
        return new WorkflowTransferException(
                "workflow.transfer.script.notFound",
                String.format("Workflow script not found: %s", key));
    }

    public static WorkflowTransferException scriptTypeUnsupported(String type) {
        return new WorkflowTransferException(
                "workflow.transfer.script.typeUnsupported",
                String.format("Unsupported workflow script reference type: %s", type));
    }

    public static WorkflowTransferException replaceTargetNotFound(String workCode) {
        return new WorkflowTransferException(
                "workflow.transfer.replace.targetNotFound",
                String.format("Replace import target workflow not found: %s", workCode));
    }
}
