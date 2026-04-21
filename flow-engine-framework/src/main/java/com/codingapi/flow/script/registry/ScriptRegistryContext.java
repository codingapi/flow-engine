package com.codingapi.flow.script.registry;

import lombok.Getter;

/**
 * 脚本注册单例
 * <p>
 * 提供获取各类默认脚本的能力，默认使用 {@link DefaultScriptRegistry}。
 * 用户可通过 {@link #setRegistry(IScriptRegistry)} 替换默认实现。
 * <p>
 * 使用示例：
 * <pre>
 * // 替换默认脚本
 * ScriptRegistry.getInstance().setRegistry(new CustomScriptRegistry());
 *
 * // 获取默认脚本
 * String script = ScriptRegistry.getInstance().getRouterScript();
 * </pre>
 */
@Getter
public class ScriptRegistryContext {

    private static final ScriptRegistryContext INSTANCE = new ScriptRegistryContext();

    private IScriptRegistry registry = new DefaultScriptRegistry();

    private ScriptRegistryContext() {
    }

    /**
     * 获取单例实例
     */
    public static ScriptRegistryContext getInstance() {
        return INSTANCE;
    }

    /**
     * 设置自定义脚本注册实现
     *
     * @param registry 自定义脚本注册实现，不能为 null
     */
    public void setRegistry(IScriptRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        this.registry = registry;
    }

    /**
     * 获取路由脚本
     */
    public String getRouterScript() {
        return registry.getRouterScript();
    }

    /**
     * 获取节点标题脚本
     */
    public String getNodeTitleScript() {
        return registry.getNodeTitleScript();
    }

    /**
     * 获取条件脚本
     */
    public String getConditionScript() {
        return registry.getConditionScript();
    }

    /**
     * 获取触发器脚本
     */
    public String getTriggerScript() {
        return registry.getTriggerScript();
    }

    /**
     * 获取子流程脚本
     */
    public String getSubProcessScript() {
        return registry.getSubProcessScript();
    }

    /**
     * 获取操作者加载脚本
     */
    public String getOperatorLoadScript() {
        return registry.getOperatorLoadScript();
    }

    /**
     * 获取操作者匹配脚本
     */
    public String getOperatorMatchScript() {
        return registry.getOperatorMatchScript();
    }

    /**
     * 获取错误触发脚本
     */
    public String getErrorTriggerScript() {
        return registry.getErrorTriggerScript();
    }

    /**
     * 获取自定义动作脚本
     */
    public String getActionCustomScript() {
        return registry.getActionCustomScript();
    }

    /**
     * 获取拒绝动作脚本
     */
    public String getActionRejectScript() {
        return registry.getActionRejectScript();
    }
}
