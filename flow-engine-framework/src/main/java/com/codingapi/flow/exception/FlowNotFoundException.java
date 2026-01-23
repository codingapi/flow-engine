package com.codingapi.flow.exception;

/**
 * Flow not found exception
 * <p>
 * Thrown when a requested resource does not exist
 * For example: workflow definition not found, flow record not found, node not found, etc.
 *
 * @since 1.0.0
 */
public class FlowNotFoundException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowNotFoundException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowNotFoundException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Workflow definition not found
     *
     * @param workflowId workflow ID
     * @return exception
     */
    public static FlowNotFoundException workflow(String workflowId) {
        return new FlowNotFoundException("notFound.workflow.definition",
                String.format("Workflow definition not found: %s", workflowId));
    }

    /**
     * Flow record not found
     *
     * @param recordId record ID
     * @return exception
     */
    public static FlowNotFoundException record(long recordId) {
        return new FlowNotFoundException("notFound.record.id",
                String.format("Flow record not found: %d", recordId));
    }

    /**
     * Node not found
     *
     * @param nodeId node ID
     * @return exception
     */
    public static FlowNotFoundException node(String nodeId) {
        return new FlowNotFoundException("notFound.node.id", String.format("Node not found: %s", nodeId));
    }

    /**
     * Operator not found
     *
     * @param operatorId operator ID
     * @return exception
     */
    public static FlowNotFoundException operator(long operatorId) {
        return new FlowNotFoundException("notFound.operator.id",
                String.format("Operator not found: %d", operatorId));
    }

    /**
     * Action not found
     *
     * @param actionId action ID
     * @return exception
     */
    public static FlowNotFoundException action(String actionId) {
        return new FlowNotFoundException("notFound.action.id", String.format("Action not found: %s", actionId));
    }
}
