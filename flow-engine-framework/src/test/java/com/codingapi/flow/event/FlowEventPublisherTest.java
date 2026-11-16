package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.DomainEvent;
import com.codingapi.springboot.framework.event.IEvent;
import com.codingapi.springboot.framework.event.SpringEventInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowEventPublisherTest {

    private final List<IEvent> capturedEvents = new ArrayList<>();

    private static class TestFlowEvent implements IFlowEvent {

        private final String eventKey;
        private final boolean mock;

        private TestFlowEvent(String eventKey, boolean mock) {
            this.eventKey = eventKey;
            this.mock = mock;
        }

        @Override
        public boolean isMock() {
            return mock;
        }

        @Override
        public String eventKey() {
            return eventKey;
        }
    }

    private static class RecordPublisher implements IFlowEventPublisher {

        private final List<IFlowEvent> events = new ArrayList<>();

        @Override
        public void push(IFlowEvent event) {
            events.add(event);
        }
    }

    @AfterEach
    void clearEventContext() throws Exception {
        Class<?> clazz = Class.forName("com.codingapi.springboot.framework.event.DomainEventContext");
        java.lang.reflect.Method getInstance = clazz.getDeclaredMethod("getInstance");
        getInstance.setAccessible(true);
        Object instance = getInstance.invoke(null);
        Field field = clazz.getDeclaredField("context");
        field.setAccessible(true);
        field.set(instance, null);
    }

    /**
     * 测试目标：未注册发布器实现时，事件推送仍走底层 EventPusher，行为与旧版本一致。
     * 前置条件：未注册 IFlowEventPublisher。
     * 执行步骤：通过门面推送一个事件。
     * 期望断言：事件被发布到底层事件框架。
     */
    @Test
    void shouldDelegateToEventPusherWhenNoPublisherRegistered() throws Exception {
        initEventCapture();

        TestFlowEvent event = new TestFlowEvent("process-1", false);
        FlowEventPublisher.push(event);

        assertEquals(1, capturedEvents.size(), "未注册发布器时应回退到底层 EventPusher");
        assertTrue(capturedEvents.get(0) == event, "推送的应为同一个事件实例");
    }

    /**
     * 测试目标：注册发布器实现后，事件由实现投递，不再直接进入底层事件框架。
     * 前置条件：调用 register 注册自定义发布器。
     * 执行步骤：通过门面推送事件。
     * 期望断言：实现收到事件，底层事件框架无感知。
     */
    @Test
    void shouldDelegateToRegisteredPublisher() throws Exception {
        initEventCapture();
        RecordPublisher publisher = new RecordPublisher();
        FlowEventPublisher.register(publisher);
        try {
            TestFlowEvent event = new TestFlowEvent("process-2", true);
            FlowEventPublisher.push(event);

            assertEquals(1, publisher.events.size(), "应交由注册的实现投递");
            assertTrue(publisher.events.get(0) == event);
            assertTrue(capturedEvents.isEmpty(), "注册实现后不应再直接推送到底层框架");
        } finally {
            FlowEventPublisher.unregister(publisher);
        }
    }

    /**
     * 测试目标：注销发布器实现后，事件推送恢复为底层 EventPusher。
     * 前置条件：先注册再注销自定义发布器。
     * 执行步骤：通过门面推送事件。
     * 期望断言：重新回退到底层事件框架。
     */
    @Test
    void shouldFallbackToEventPusherAfterUnregister() throws Exception {
        initEventCapture();
        RecordPublisher publisher = new RecordPublisher();
        FlowEventPublisher.register(publisher);
        FlowEventPublisher.unregister(publisher);

        FlowEventPublisher.push(new TestFlowEvent("process-3", false));

        assertTrue(publisher.events.isEmpty(), "注销后不应再调用已注销的实现");
        assertEquals(1, capturedEvents.size(), "注销后应回退到底层 EventPusher");
    }

    private void initEventCapture() throws Exception {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.doAnswer(invocation -> {
            DomainEvent domainEvent = invocation.getArgument(0);
            capturedEvents.add(domainEvent.getEvent());
            return null;
        }).when(context).publishEvent(Mockito.any(ApplicationEvent.class));
        new SpringEventInitializer(context).afterPropertiesSet();
    }
}
