package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.EventPusher;

/**
 * 流程事件发布门面。
 *
 * <p>引擎内统一使用该门面推送事件（不再直接调用 {@code EventPusher.push}）：</p>
 * <ul>
 *     <li>已注册 {@link IFlowEventPublisher}（如 starter 提供的实现）时，交由实现投递，
 *     可获得「事务提交后投递 + 按流程串行」的语义；</li>
 *     <li>未注册时回退为 {@code EventPusher.push}，与旧版本行为完全一致
 *     （仅使用 framework、单元测试等场景）。</li>
 * </ul>
 */
public class FlowEventPublisher {

    private static volatile IFlowEventPublisher publisher;

    private FlowEventPublisher() {
    }

    /**
     * 注册事件发布器实现（由 starter 初始化时调用）。
     *
     * @param eventPublisher 事件发布器
     */
    public static void register(IFlowEventPublisher eventPublisher) {
        publisher = eventPublisher;
    }

    /**
     * 注销事件发布器实现，恢复为直接调用 {@code EventPusher}（测试或关闭能力时使用）。
     *
     * @param eventPublisher 待注销的事件发布器，仅当其为当前实现时才生效
     */
    public static void unregister(IFlowEventPublisher eventPublisher) {
        if (publisher == eventPublisher) {
            publisher = null;
        }
    }

    /**
     * 推送流程事件。
     *
     * @param event 流程事件
     */
    public static void push(IFlowEvent event) {
        IFlowEventPublisher eventPublisher = publisher;
        if (eventPublisher == null) {
            EventPusher.push(event);
            return;
        }
        eventPublisher.push(event);
    }
}
