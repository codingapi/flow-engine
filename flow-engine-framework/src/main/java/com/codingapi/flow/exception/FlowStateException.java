package com.codingapi.flow.exception;

/**
 * 流程状态异常
 * <p>
 * 当流程状态不允许执行当前操作时抛出此异常
 * 例如：流程已完成不允许再操作、记录已处理不允许重复处理等
 *
 * @since 1.0.0
 */
public class FlowStateException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_STATE_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowStateException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowStateException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowStateException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }

    /**
     * 记录已完成，不允许操作
     *
     * @return 异常
     */
    public static FlowStateException recordAlreadyDone() {
        return new FlowStateException("001", "record has done");
    }

    /**
     * 操作者不匹配
     *
     * @return 异常
     */
    public static FlowStateException operatorNotMatch() {
        return new FlowStateException("002", "currentOperator not match");
    }

    /**
     * 流程已完成
     *
     * @return 异常
     */
    public static FlowStateException workflowAlreadyFinished() {
        return new FlowStateException("003", "workflow already finished");
    }

    /**
     * 流程已终止
     *
     * @return 异常
     */
    public static FlowStateException workflowAlreadyTerminated() {
        return new FlowStateException("004", "workflow already terminated");
    }

    /**
     * 无效的状态转换
     *
     * @param fromState 当前状态
     * @param toState   目标状态
     * @return 异常
     */
    public static FlowStateException invalidStateTransition(String fromState, String toState) {
        return new FlowStateException("005",
            String.format("invalid state transition from '%s' to '%s'", fromState, toState));
    }
}
