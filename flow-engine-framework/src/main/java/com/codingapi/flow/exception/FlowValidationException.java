package com.codingapi.flow.exception;

/**
 * 流程参数验证异常
 * <p>
 * 当流程引擎的输入参数不符合要求时抛出此异常
 * 例如：必填参数为空、参数格式不正确等
 *
 * @since 1.0.0
 */
public class FlowValidationException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_VALIDATION_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowValidationException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }


    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowValidationException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowValidationException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }

    /**
     * 必填字段为空
     *
     * @param fieldName 字段名称
     * @return 异常
     */
    public static FlowValidationException required(String fieldName) {
        return new FlowValidationException(fieldName, "can not be null");
    }

    /**
     * 字段值不能为空
     *
     * @param fieldName 字段名称
     * @return 异常
     */
    public static FlowValidationException notEmpty(String fieldName) {
        return new FlowValidationException(fieldName, "can not be empty");
    }
}
