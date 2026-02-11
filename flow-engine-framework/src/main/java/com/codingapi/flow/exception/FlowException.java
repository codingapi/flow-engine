package com.codingapi.flow.exception;

import com.codingapi.springboot.framework.exception.LocaleMessageException;

/**
 * 流程引擎框架异常基类
 * <p>
 * 所有流程引擎相关的异常都应该继承此类
 *
 * @since 1.0.0
 */
public abstract class FlowException extends LocaleMessageException {

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowException(String errorCode, String message) {
        super(errorCode,message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowException(String errorCode, String message, Throwable cause) {
        super(errorCode,message, cause);
    }

}
