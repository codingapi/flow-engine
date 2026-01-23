package com.codingapi.flow.exception;

/**
 * Flow parameter validation exception
 * <p>
 * Thrown when input parameters to the flow engine do not meet requirements
 * For example: required parameter is empty, parameter format is incorrect, etc.
 *
 * @since 1.0.0
 */
public class FlowValidationException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowValidationException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowValidationException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Required field is empty
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowValidationException required(String fieldName) {
        return new FlowValidationException("validation.field.required",
                String.format("Required field %s cannot be empty", fieldName));
    }

    /**
     * Field value cannot be empty
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowValidationException notEmpty(String fieldName) {
        return new FlowValidationException("validation.field.notEmpty",
                String.format("Field %s value cannot be empty", fieldName));
    }

    /**
     * Max size must be positive
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowValidationException mustBePositive(String fieldName) {
        return new FlowValidationException("validation.value.mustBePositive",
                String.format("%s must be positive", fieldName));
    }
}
