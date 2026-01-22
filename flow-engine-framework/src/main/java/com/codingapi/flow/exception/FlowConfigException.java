package com.codingapi.flow.exception;

/**
 * 流程配置异常
 * <p>
 * 当流程配置不正确时抛出此异常
 * 例如：节点配置错误、边配置错误、策略配置错误等
 *
 * @since 1.0.0
 */
public class FlowConfigException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_CONFIG_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowConfigException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowConfigException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     * @param cause     原因
     */
    public FlowConfigException(String errorCode, String message, Throwable cause) {
        super(ERROR_CODE_PREFIX + errorCode, message, cause);
    }

    /**
     * 策略不能为空
     *
     * @return 异常
     */
    public static FlowConfigException strategiesNotNull() {
        return new FlowConfigException("001", "strategies can not be null");
    }

    /**
     * 动作不能为空
     *
     * @return 异常
     */
    public static FlowConfigException actionsNotNull() {
        return new FlowConfigException("002", "actions can not be null");
    }

    /**
     * 节点配置错误
     *
     * @param nodeName 节点名称
     * @param reason   原因
     * @return 异常
     */
    public static FlowConfigException nodeConfigError(String nodeName, String reason) {
        return new FlowConfigException("003",
                String.format("node '%s' configuration error: %s", nodeName, reason));
    }

    /**
     * 边配置错误
     *
     * @param reason 原因
     * @return 异常
     */
    public static FlowConfigException edgeConfigError(String reason) {
        return new FlowConfigException("004", String.format("edge configuration error: %s", reason));
    }

    /**
     * 表单配置错误
     *
     * @param formName 表单名称
     * @param reason   原因
     * @return 异常
     */
    public static FlowConfigException formConfigError(String formName, String reason) {
        return new FlowConfigException("005",
                String.format("form '%s' configuration error: %s", formName, reason));
    }

    /**
     * 视图不能为空
     *
     * @return 异常
     */
    public static FlowConfigException viewNotNull() {
        return new FlowConfigException("006", "view can not be null");
    }

    /**
     * 并行结束节点不能为空
     *
     * @return 异常
     */
    public static FlowConfigException parallelEndNodeNotNull() {
        return new FlowConfigException("007", "parallel end node is null");
    }

    /**
     * 当前节点不能为空
     *
     * @return 异常
     */
    public static FlowConfigException currentNodeNotNull() {
        return new FlowConfigException("008", "currentNode is null");
    }
}
