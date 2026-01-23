package com.codingapi.flow.exception;

/**
 * Flow configuration exception
 * <p>
 * Thrown when the flow configuration is incorrect
 * For example: node configuration error, edge configuration error, strategy configuration error, etc.
 *
 * @since 1.0.0
 */
public class FlowConfigException extends FlowException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     */
    public FlowConfigException(String code, String message) {
        super(code, message);
    }

    /**
     * Constructor
     *
     * @param code    error code
     * @param message error message
     * @param cause   cause
     */
    public FlowConfigException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Strategies cannot be null
     *
     * @return exception
     */
    public static FlowConfigException strategiesNotNull() {
        return new FlowConfigException("config.node.strategies.required", "Strategies cannot be null");
    }

    /**
     * Actions cannot be null
     *
     * @return exception
     */
    public static FlowConfigException actionsNotNull() {
        return new FlowConfigException("config.node.actions.required", "Actions cannot be null");
    }

    /**
     * Node configuration error
     *
     * @param nodeName node name
     * @param reason   reason
     * @return exception
     */
    public static FlowConfigException nodeConfigError(String nodeName, String reason) {
        return new FlowConfigException("config.node.error",
                String.format("Node '%s' configuration error: %s", nodeName, reason));
    }

    /**
     * Edge configuration error
     *
     * @param reason reason
     * @return exception
     */
    public static FlowConfigException edgeConfigError(String reason) {
        return new FlowConfigException("config.edge.error", String.format("Edge configuration error: %s", reason));
    }

    /**
     * Form configuration error
     *
     * @param formName form name
     * @param reason   reason
     * @return exception
     */
    public static FlowConfigException formConfigError(String formName, String reason) {
        return new FlowConfigException("config.form.error",
                String.format("Form '%s' configuration error: %s", formName, reason));
    }

    /**
     * View cannot be null
     *
     * @return exception
     */
    public static FlowConfigException viewNotNull() {
        return new FlowConfigException("config.view.required", "View cannot be null");
    }

    /**
     * Parallel end node cannot be null
     *
     * @return exception
     */
    public static FlowConfigException parallelEndNodeNotNull() {
        return new FlowConfigException("config.parallel.endNode.required", "Parallel end node cannot be null");
    }

    /**
     * Current node cannot be null
     *
     * @return exception
     */
    public static FlowConfigException currentNodeNotNull() {
        return new FlowConfigException("config.node.current.required", "Current node cannot be null");
    }

    /**
     * Router node script is null
     *
     * @return exception
     */
    public static FlowConfigException routerNodeScriptNull() {
        return new FlowConfigException("config.router.script.required", "Router node script cannot be null");
    }

    /**
     * Repository not registered
     *
     * @return exception
     */
    public static FlowConfigException repositoryNotRegistered() {
        return new FlowConfigException("config.repository.notRegistered", "Flow repository components not registered");
    }
}
