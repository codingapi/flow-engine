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
     * Operator count exceeds the max count
     *
     * @param actionType       action type (delegate/transfer/addAudit)
     * @param maxOperatorCount max operator count
     * @return exception
     */
    public static FlowExecutionException operatorCountExceeded(String actionType, int maxOperatorCount) {
        return new FlowExecutionException("execution." + actionType + ".operatorCountExceeded",
                String.format("Operator count exceeds the max count %d of the %s action", maxOperatorCount, actionType));
    }

    /**
     * Add audit target is already in the todo list of the current node instance
     * <p>
     * Adding an approver who already holds a pending record would create duplicated
     * todos for the same operator on the same node, so it is rejected.
     *
     * @param operatorId target operator id
     * @return exception
     */
    public static FlowExecutionException addAuditOperatorAlreadyInTodo(long operatorId) {
        return new FlowExecutionException("execution.addAudit.operatorAlreadyInTodo",
                String.format("Operator %d is already a pending approver of the current node", operatorId));
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

    /**
     * Reduce audit is not supported by the current multi operator audit strategy
     * <p>
     * Only sequence (SEQUENCE) and merge (MERGE) multi operator audit nodes need to
     * reduce approvers; ANY and RANDOM_ONE complete with a single approval, so
     * removing an approver has no business meaning.
     *
     * @return exception
     */
    public static FlowExecutionException reduceAuditUnsupported() {
        return new FlowExecutionException("execution.reduceAudit.unsupported",
                "Reduce audit is only supported by sequence or merge multi-operator audit strategy");
    }

    /**
     * Reduce audit target is not a pending approver of the current node instance
     *
     * @param operatorId target operator id
     * @return exception
     */
    public static FlowExecutionException reduceAuditTargetNotFound(long operatorId) {
        return new FlowExecutionException("execution.reduceAudit.targetNotFound",
                String.format("Operator %d is not a pending approver of the current node", operatorId));
    }

    /**
     * Reduce audit is not allowed to remove the operator who is executing the action
     *
     * @return exception
     */
    public static FlowExecutionException reduceAuditOwnRecord() {
        return new FlowExecutionException("execution.reduceAudit.ownRecord",
                "Reduce audit is not allowed to remove the current operator, use pass, reject or transfer instead");
    }

    /**
     * Reduce audit would leave the node without any pending approval
     * <p>
     * Node completion is only evaluated when an approval action is submitted. If the
     * node is already satisfied (or has no pending approvers) after the removal, no
     * action will ever trigger the completion and the flow stalls forever.
     *
     * @return exception
     */
    public static FlowExecutionException reduceAuditNoRemainingApproval() {
        return new FlowExecutionException("execution.reduceAudit.noRemainingApproval",
                "Reduce audit rejected: the node still requires at least one pending approval to finish");
    }

    /**
     * Reduce audit is not allowed when the current node has only one pending approver
     * <p>
     * A reduce audit always removes someone else from the node, so a single pending
     * approver means there is nobody left to remove.
     *
     * @return exception
     */
    public static FlowExecutionException reduceAuditSingleApprover() {
        return new FlowExecutionException("execution.reduceAudit.singleApprover",
                "Reduce audit rejected: the current node has only one pending approver");
    }
}
