package com.codingapi.flow.exception;

/**
 * 流程权限异常
 * <p>
 * 当操作者没有权限执行当前操作时抛出此异常
 * 例如：字段只读不允许修改、无权操作该流程等
 *
 * @since 1.0.0
 */
public class FlowPermissionException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_PERMISSION_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowPermissionException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowPermissionException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowPermissionException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }

    /**
     * 字段只读
     *
     * @param fieldName 字段名称
     * @return 异常
     */
    public static FlowPermissionException fieldReadOnly(String fieldName) {
        return new FlowPermissionException("001", String.format("field '%s' is readonly", fieldName));
    }

    /**
     * 字段不存在
     *
     * @param fieldName 字段名称
     * @return 异常
     */
    public static FlowPermissionException fieldNotFound(String fieldName) {
        return new FlowPermissionException("002", String.format("field '%s' not found", fieldName));
    }

    /**
     * 无权操作
     *
     * @param operation 操作名称
     * @return 异常
     */
    public static FlowPermissionException accessDenied(String operation) {
        return new FlowPermissionException("003", String.format("access denied for operation: %s", operation));
    }

    /**
     * 无权操作该流程
     *
     * @param operatorId 操作者ID
     * @return 异常
     */
    public static FlowPermissionException noPermission(long operatorId) {
        return new FlowPermissionException("004",
            String.format("operator %d has no permission to access this resource", operatorId));
    }
}
