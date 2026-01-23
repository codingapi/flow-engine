package com.codingapi.flow.exception;

/**
 * Flow execution exception
 * <p>
 * Thrown when an error occurs during flow execution
 * For example: script execution error, node execution error, action execution error, etc.
 *
 * @since 1.0.0
 */
public class FlowExecutionException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowExecutionException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowExecutionException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Script execution error
     *
     * @param scriptType script type
     * @param cause      cause
     * @return exception
     */
    public static FlowExecutionException scriptExecutionError(String scriptType, Throwable cause) {
        return new FlowExecutionException("execution.script.error",
                String.format("Script execution error: %s", scriptType), cause);
    }

    /**
     * Node execution error
     *
     * @param nodeId node ID
     * @param cause  cause
     * @return exception
     */
    public static FlowExecutionException nodeExecutionError(String nodeId, Throwable cause) {
        return new FlowExecutionException("execution.node.error",
                String.format("Node execution error: %s", nodeId), cause);
    }

    /**
     * Action execution error
     *
     * @param actionId action ID
     * @param cause    cause
     * @return exception
     */
    public static FlowExecutionException actionExecutionError(String actionId, Throwable cause) {
        return new FlowExecutionException("execution.action.error",
                String.format("Action execution error: %s", actionId), cause);
    }

    /**
     * Workflow execution error
     *
     * @param processId process instance ID
     * @param cause     cause
     * @return exception
     */
    public static FlowExecutionException workflowExecutionError(String processId, Throwable cause) {
        return new FlowExecutionException("execution.workflow.error",
                String.format("Workflow execution error: %s", processId), cause);
    }

    /**
     * Database operation error
     *
     * @param operation operation type
     * @param cause     cause
     * @return exception
     */
    public static FlowExecutionException databaseError(String operation, Throwable cause) {
        return new FlowExecutionException("execution.database.error",
                String.format("Database operation error: %s", operation), cause);
    }

    /**
     * Router node not found
     *
     * @param nodeId node ID
     * @return exception
     */
    public static FlowExecutionException routerNodeNotFound(String nodeId) {
        return new FlowExecutionException("execution.router.nodeNotFound",
                String.format("Router node not found: %s", nodeId));
    }

    /**
     * Create record size error
     *
     * @return exception
     */
    public static FlowExecutionException createRecordSizeError() {
        return new FlowExecutionException("execution.createRecord.sizeError",
                "Create record error: record size must be 1");
    }

    /**
     * Operator not in scope
     *
     * @param actionType action type (delegate/transfer/addAudit)
     * @return exception
     */
    public static FlowExecutionException operatorNotInScope(String actionType) {
        return new FlowExecutionException("execution." + actionType + ".operatorNotInScope",
                String.format("Operator is not in the scope of the %s action", actionType));
    }

    /**
     * Custom action next not found
     *
     * @return exception
     */
    public static FlowExecutionException customActionNextNotFound() {
        return new FlowExecutionException("execution.custom.action.nextNotFound", "Next action not found");
    }
}
