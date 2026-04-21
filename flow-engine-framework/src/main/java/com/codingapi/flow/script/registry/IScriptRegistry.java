package com.codingapi.flow.script.registry;

/**
 * 脚本注册接口
 * <p>
 * 定义获取各类默认脚本的方法，实现此接口可以自定义默认脚本行为。
 * 通过 {@link ScriptRegistryContext} 单例使用。
 */
public interface IScriptRegistry {

    /**
     * 获取路由脚本
     */
    String getRouterScript();

    /**
     * 获取节点标题脚本
     */
    String getNodeTitleScript();

    /**
     * 获取条件脚本
     */
    String getConditionScript();

    /**
     * 获取触发器脚本
     */
    String getTriggerScript();

    /**
     * 获取子流程脚本
     */
    String getSubProcessScript();

    /**
     * 获取操作者加载脚本
     */
    String getOperatorLoadScript();

    /**
     * 获取操作者匹配脚本
     */
    String getOperatorMatchScript();

    /**
     * 获取错误触发脚本
     */
    String getErrorTriggerScript();

    /**
     * 获取自定义动作脚本
     */
    String getActionCustomScript();

    /**
     * 获取拒绝动作脚本
     */
    String getActionRejectScript();
}
