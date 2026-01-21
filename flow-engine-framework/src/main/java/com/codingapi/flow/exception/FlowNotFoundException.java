package com.codingapi.flow.exception;

/**
 * 流程未找到异常
 * <p>
 * 当请求的资源不存在时抛出此异常
 * 例如：流程定义不存在、流程记录不存在、节点不存在等
 *
 * @since 1.0.0
 */
public class FlowNotFoundException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "FLOW_NOT_FOUND_";

    /**
     * 构造函数
     *
     * @param message 错误信息
     */
    public FlowNotFoundException(String message) {
        super(ERROR_CODE_PREFIX + "000", message);
    }

    /**
     * 构造函数
     *
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     */
    public FlowNotFoundException(String resourceType, Object resourceId) {
        super(ERROR_CODE_PREFIX + "000", String.format("%s not found: %s", resourceType, resourceId));
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public FlowNotFoundException(String errorCode, String message) {
        super(ERROR_CODE_PREFIX + errorCode, message);
    }

    /**
     * 流程定义不存在
     *
     * @param workflowId 流程ID
     * @return 异常
     */
    public static FlowNotFoundException workflow(String workflowId) {
        return new FlowNotFoundException("Workflow", workflowId);
    }

    /**
     * 流程记录不存在
     *
     * @param recordId 记录ID
     * @return 异常
     */
    public static FlowNotFoundException record(long recordId) {
        return new FlowNotFoundException("FlowRecord", recordId);
    }

    /**
     * 节点不存在
     *
     * @param nodeId 节点ID
     * @return 异常
     */
    public static FlowNotFoundException node(String nodeId) {
        return new FlowNotFoundException("Node", nodeId);
    }

    /**
     * 操作者不存在
     *
     * @param operatorId 操作者ID
     * @return 异常
     */
    public static FlowNotFoundException operator(long operatorId) {
        return new FlowNotFoundException("Operator", operatorId);
    }

    /**
     * 动作不存在
     *
     * @param actionId 动作ID
     * @return 异常
     */
    public static FlowNotFoundException action(String actionId) {
        return new FlowNotFoundException("Action", actionId);
    }
}
