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
