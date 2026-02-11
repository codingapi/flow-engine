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
     * node field is empty
     *
     * @param fieldName node field name
     * @return exception
     */
    public static FlowValidationException nodeRequired(String fieldName) {
        return new FlowValidationException("validation.node."+fieldName,
                String.format("Required field %s cannot be empty", fieldName));
    }


    /**
     * Field is read-only
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowValidationException fieldReadOnly(String fieldName) {
        return new FlowValidationException("validation.field.readOnly",
                String.format("Field '%s' is read-only and cannot be modified", fieldName));
    }

    /**
     * Field not found
     *
     * @param fieldName field name
     * @return exception
     */
    public static FlowValidationException fieldNotFound(String fieldName) {
        return new FlowValidationException("validation.field.notFound",
                String.format("Field '%s' does not exist", fieldName));
    }

    /**
     * workflow field is empty
     *
     * @param fieldName node field name
     * @return exception
     */
    public static FlowValidationException workflowRequired(String fieldName) {
        return new FlowValidationException("validation.workflow."+fieldName,
                String.format("Required field %s cannot be empty", fieldName));
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
