package com.codingapi.flow.event;

/**
 * 流程事件发布器（SPI）。
 *
 * <p>引擎内部所有事件推送都经过 {@link FlowEventPublisher}，由该接口的实现决定投递时机与顺序。
 * 由 starter 模块提供 Spring 实现（事务提交后投递、按流程串行）；未注册实现时
 * {@link FlowEventPublisher} 回退为直接调用底层 {@code EventPusher}，行为与旧版本一致。</p>
 */
public interface IFlowEventPublisher {

    /**
     * 发布流程事件。
     *
     * <p>实现方须保证：有活动事务时在事务提交后才真正投递、同一 {@link IFlowEvent#eventKey()}
     * 的事件按推送顺序投递。</p>
     *
     * @param event 流程事件
     */
    void push(IFlowEvent event);

}
