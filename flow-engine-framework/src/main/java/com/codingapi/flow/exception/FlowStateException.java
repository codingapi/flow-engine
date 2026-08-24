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
     * Repository not registered
     *
     * @return exception
     */
    public static FlowStateException repositoryNotRegistered() {
        return new FlowStateException("state.repository.notRegistered", "Flow repository components not registered");
    }

    /**
     * Current node cannot be null
     *
     * @return exception
     */
    public static FlowStateException currentNodeNotNull() {
        return new FlowStateException("state.node.current.required", "Current node cannot be null");
    }

    /**
     * Edge configuration error
     *
     * @param reason reason
     * @return exception
     */
    public static FlowStateException edgeConfigError(String reason) {
        return new FlowStateException("state.edge.error", String.format("Edge configuration error: %s", reason));
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
     *
     * @return exception
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
     *
     * Workflow is already disable
     *
     * @param workflowId workflowId
     * @return exception
     */
    public static FlowStateException workflowAlreadyDisable(String workflowId) {
        return new FlowStateException("state.workflow.disable",
                String.format("Workflow is disable: %s", workflowId));
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
     *
     * @return exception
     */
    public static FlowStateException nodeNotSupportRevoke() {
        return new FlowStateException("node.notSupportRevoke", "Node not support revoke");
    }

    /**
     * Record not support urge
     *
     * @return exception
     */
    public static FlowStateException recordLimitUrgeError() {
        return new FlowStateException("record.urge.limit", "record urge limit error");
    }

    /**
     * Record not support delete (not a todo / finished / revoked)
     *
     * @return exception
     */
    public static FlowStateException recordNotSupportDelete() {
        return new FlowStateException("state.record.notSupportDelete", "Flow record is not a todo, delete not allowed");
    }

    /**
     * Record is not on the start node, delete not allowed
     *
     * @return exception
     */
    public static FlowStateException nodeNotStartNode() {
        return new FlowStateException("state.node.notStart", "Flow record is not on the start node, delete not allowed");
    }

    /**
     * Flow already running (has subsequent records), delete not allowed
     *
     * @return exception
     */
    public static FlowStateException recordAlreadyRunning() {
        return new FlowStateException("state.record.alreadyRunning", "Flow is already running, delete not allowed");
    }

    /**
     * Sub process group not support reset (no passed group / group not finished / already superseded)
     *
     * @return exception
     */
    public static FlowStateException subProcessNotSupportReset() {
        return new FlowStateException("state.subProcess.notSupportReset",
                "Sub process group not support reset, only a passed group with all instances finished can be reset");
    }

    /**
     * Sub process reset not allowed while any group is waiting
     *
     * @return exception
     */
    public static FlowStateException subProcessResetWaiting() {
        return new FlowStateException("state.subProcess.resetWaiting",
                "Sub process reset not allowed while any sub process group is waiting");
    }

    /**
     * Sub process reset only allowed on the locked merge node record
     *
     * @return exception
     */
    public static FlowStateException subProcessResetMergeNodeOnly() {
        return new FlowStateException("state.subProcess.resetMergeNodeOnly",
                "Sub process reset only allowed on the todo record of the locked merge node");
    }
}
