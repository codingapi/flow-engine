package com.codingapi.flow.exception;

/**
 * 流程引擎框架异常基类
 * <p>
 * 所有流程引擎相关的异常都应该继承此类
 *
 * @since 1.0.0
 */
public abstract class FlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", errorCode, getMessage());
    }
}
