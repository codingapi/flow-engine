package com.codingapi.flow.exception;

/**
 * 流程执行异常
 * <p>
 * 当流程执行过程中发生错误时抛出此异常
 * 例如：脚本执行错误、节点执行错误、动作执行错误等
 *
 * @since 1.0.0
 */
public class FlowExecutionException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_EXECUTION_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowExecutionException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }

    /**
     * 构造函数
     *
     * @param message 错误信息
     * @param cause   原因
     */
    public FlowExecutionException(String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + "000", message, cause);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowExecutionException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowExecutionException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }

    /**
     * 脚本执行错误
     *
     * @param scriptType 脚本类型
     * @param cause      原因
     * @return 异常
     */
    public static FlowExecutionException scriptExecutionError(String scriptType, Throwable cause) {
        return new FlowExecutionException("001",
                String.format("script execution error: %s", scriptType), cause);
    }

    /**
     * 节点执行错误
     *
     * @param nodeId 节点ID
     * @param cause  原因
     * @return 异常
     */
    public static FlowExecutionException nodeExecutionError(String nodeId, Throwable cause) {
        return new FlowExecutionException("002",
                String.format("node execution error: %s", nodeId), cause);
    }

    /**
     * 动作执行错误
     *
     * @param actionId 动作ID
     * @param cause    原因
     * @return 异常
     */
    public static FlowExecutionException actionExecutionError(String actionId, Throwable cause) {
        return new FlowExecutionException("003",
                String.format("action execution error: %s", actionId), cause);
    }

    /**
     * 流程执行错误
     *
     * @param processId 流程实例ID
     * @param cause     原因
     * @return 异常
     */
    public static FlowExecutionException workflowExecutionError(String processId, Throwable cause) {
        return new FlowExecutionException("004",
                String.format("workflow execution error: %s", processId), cause);
    }

    /**
     * 数据库操作错误
     *
     * @param operation 操作类型
     * @param cause     原因
     * @return 异常
     */
    public static FlowExecutionException databaseError(String operation, Throwable cause) {
        return new FlowExecutionException("005",
                String.format("database operation error: %s", operation), cause);
    }
}
