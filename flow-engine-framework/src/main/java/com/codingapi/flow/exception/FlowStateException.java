package com.codingapi.flow.exception;

/**
 * Flow state exception
 * <p>
 * Thrown when the flow state does not allow the current operation
 * For example: workflow is finished and cannot be operated, record is processed and cannot be processed again, etc.
 *
 * @since 1.0.0
 */
public class FlowStateException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowStateException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowStateException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Record is already done, operation not allowed
     *
     * @return exception
     */
    public static FlowStateException recordAlreadyDone() {
        return new FlowStateException("state.record.alreadyDone", "Flow record is already completed, duplicate operation not allowed");
    }


    /**
     * Record is already todo, operation not allowed
     * @return  exception
     */
    public static FlowStateException recordAlreadyTodo() {
        return new FlowStateException("state.record.alreadyTodo", "Flow record is already todo, duplicate operation not allowed");
    }
    /**
     * Operator does not match
     *
     * @return exception
     */
    public static FlowStateException operatorNotMatch() {
        return new FlowStateException("state.operator.notMatch", "Current operator has no permission to process this flow record");
    }

    /**
     * Workflow is already finished
     *
     * @return exception
     */
    public static FlowStateException workflowAlreadyFinished() {
        return new FlowStateException("state.workflow.alreadyFinished", "Workflow is finished, further operation not allowed");
    }

    /**
     * Workflow is already terminated
     *
     * @return exception
     */
    public static FlowStateException workflowAlreadyTerminated() {
        return new FlowStateException("state.workflow.alreadyTerminated", "Workflow is terminated, further operation not allowed");
    }

    /**
     * Invalid state transition
     *
     * @param fromState current state
     * @param toState   target state
     * @return exception
     */
    public static FlowStateException invalidStateTransition(String fromState, String toState) {
        return new FlowStateException("state.transition.invalid",
                String.format("Invalid state transition: from '%s' to '%s'", fromState, toState));
    }




    /**
     * Record not support revoke
     *
     * @return exception
     */
    public static FlowStateException recordNotSupportRevoke() {
        return new FlowStateException("record.notSupportRevoke", "record not support revoke");
    }


    /**
     * Node not support revoke
     * @return exception
     */
    public static FlowStateException nodeNotSupportRevoke() {
        return new FlowStateException("node.notSupportRevoke", "Node not support revoke");
    }

    /**
     * Record not support urge
     * @return exception
     */
    public static FlowStateException recordNotSupportUrge() {
        return new FlowStateException("record.notSupportUrge", "record not support urge");
    }
}
