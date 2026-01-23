package com.codingapi.flow.exception;

/**
 * Flow permission exception
 * <p>
 * Thrown when an operator does not have permission to perform the current operation
 * For example: field is read-only and cannot be modified, no permission to operate the flow, etc.
 *
 * @since 1.0.0
 */
public class FlowPermissionException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowPermissionException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowPermissionException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Field is read-only
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowPermissionException fieldReadOnly(String fieldName) {
        return new FlowPermissionException("permission.field.readOnly",
                String.format("Field '%s' is read-only and cannot be modified", fieldName));
    }

    /**
     * Field not found
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowPermissionException fieldNotFound(String fieldName) {
        return new FlowPermissionException("permission.field.notFound",
                String.format("Field '%s' does not exist", fieldName));
    }

    /**
     * Access denied
     *
     * @param operation operation name
     * @return exception
     */
    public static FlowPermissionException accessDenied(String operation) {
        return new FlowPermissionException("permission.access.denied",
                String.format("Access denied to operation: %s", operation));
    }

    /**
     * No permission to operate the flow
     *
     * @param operatorId operator ID
     * @return exception
     */
    public static FlowPermissionException noPermission(long operatorId) {
        return new FlowPermissionException("permission.access.noPermission",
                String.format("Operator %d has no permission to access this resource", operatorId));
    }
}
