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
     * remove version error
     *
     * @return exception
     */
    public static FlowExecutionException removeWorkflowError() {
        return new FlowExecutionException("execution.workflowVersion.removeError",
                "current version don't remove.");
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

    /**
     * Sub process loop detected
     * <p>
     * The same sub process node has been triggered repeatedly in the flow chain,
     * which means the sub process creates itself or an ancestor flow.
     *
     * @return exception
     */
    public static FlowExecutionException subProcessLoop() {
        return new FlowExecutionException("execution.subProcess.loop",
                "Sub process loop detected: the same sub process node is triggered repeatedly in the flow chain.");
    }

    /**
     * Sub process nesting depth exceeds the limit
     *
     * @param maxDepth max sub process depth
     * @return exception
     */
    public static FlowExecutionException subProcessMaxDepth(int maxDepth) {
        return new FlowExecutionException("execution.subProcess.maxDepth",
                String.format("Sub process nesting depth exceeds the limit: %d", maxDepth));
    }

    /**
     * Error trigger node loop detected
     * <p>
     * The error trigger jumps back to a node that has already been visited during
     * the current record generation, which would cause infinite recursion.
     *
     * @return exception
     */
    public static FlowExecutionException errorTriggerLoop() {
        return new FlowExecutionException("execution.node.errorTriggerLoop",
                "Error trigger node loop detected: the error trigger jumps back to an already visited node.");
    }

    /**
     * Error trigger recursion depth exceeds the limit
     *
     * @param maxDepth max nest depth
     * @return exception
     */
    public static FlowExecutionException errorTriggerDepthExceeded(int maxDepth) {
        return new FlowExecutionException("execution.node.errorTriggerDepth",
                String.format("Error trigger recursion depth exceeds the limit: %d", maxDepth));
    }

    /**
     * Invalid jump target
     * <p>
     * The error trigger / reject action jumps to a node type that cannot generate
     * records (e.g. notify node or sub process node), which would silently stall
     * the flow instead of producing a new todo.
     *
     * @param nodeType node type
     * @return exception
     */
    public static FlowExecutionException invalidJumpTarget(String nodeType) {
        return new FlowExecutionException("execution.node.invalidJumpTarget",
                String.format("Jump target node type is not supported for jump: %s", nodeType));
    }

    /**
     * Node execution count exceeds the limit
     * <p>
     * The same node has been executed too many times in the flow instance chain,
     * which indicates a loop chain (e.g. A -> B -> C -> B) instead of a normal
     * forward progress.
     *
     * @param maxDepth max nest depth
     * @return exception
     */
    public static FlowExecutionException nodeLoopDepthExceeded(int maxDepth) {
        return new FlowExecutionException("execution.node.loopDepth",
                String.format("Node execution count exceeds the loop guard limit: %d", maxDepth));
    }
}
