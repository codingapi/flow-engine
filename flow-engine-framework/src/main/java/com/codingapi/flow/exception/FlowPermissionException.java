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
     * Access denied
     *
     * @param operation operation name
     * @return exception
     */
    public static FlowPermissionException accessDenied(String operation) {
        return new FlowPermissionException("permission.access.denied",
                String.format("Access denied to operation: %s", operation));
    }

}
