package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.DomainEvent;
import com.codingapi.springboot.framework.event.IHandler;
import com.codingapi.springboot.framework.event.SpringDefaultEventHandler;
import com.codingapi.springboot.framework.properties.FrameworkProperties;
import com.codingapi.springboot.framework.properties.PropertiesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程事件发布器（提交后投递 + 分区串行）测试。
 */
class SpringFlowEventPublisherTest {

    private static final List<Integer> HANDLED = new CopyOnWriteArrayList<>();
    private static final List<String> HANDLED_THREADS = new CopyOnWriteArrayList<>();
    private static volatile int slowEventId = -1;
    private static volatile int failEventId = -1;

    private static ApplicationContext applicationContext;

    private SpringFlowEventPublisher publisher;

    static class TestFlowEvent implements IFlowEvent {

        private final int id;
        private final String eventKey;
        private final boolean mock;

        private TestFlowEvent(int id, String eventKey, boolean mock) {
            this.id = id;
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

    static class Recorder implements IHandler<TestFlowEvent> {

        @Override
        public void handler(TestFlowEvent event) {
            if (event.id == slowEventId) {
                sleep(80);
            }
            if (event.id == failEventId) {
                throw new IllegalStateException("handler error, eventId:" + event.id);
            }
            HANDLED.add(event.id);
            HANDLED_THREADS.add(Thread.currentThread().getName());
        }
    }

    @BeforeAll
    static void initEventFramework() throws Exception {
        PropertiesContext.getInstance().setProperties(new FrameworkProperties());
        SpringDefaultEventHandler eventHandler = new SpringDefaultEventHandler(List.of(new Recorder()));
        eventHandler.afterPropertiesSet();

        applicationContext = Mockito.mock(ApplicationContext.class);
        Mockito.doAnswer(invocation -> {
            DomainEvent domainEvent = invocation.getArgument(0);
            eventHandler.dispatch(domainEvent);
            return null;
        }).when(applicationContext).publishEvent(Mockito.any(ApplicationEvent.class));
    }

    @AfterEach
    void tearDown() {
        if (publisher != null) {
            publisher.destroy();
            publisher = null;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        HANDLED.clear();
        HANDLED_THREADS.clear();
        slowEventId = -1;
        failEventId = -1;
    }

    /**
     * 测试目标：事务内推送的事件必须等事务提交后才投递给订阅方。
     * 前置条件：存在活动事务同步。
     * 执行步骤：事务内连续推送两个事件 → 提交前观察 → afterCommit 后观察。
     * 期望断言：提交前无任何 handler 执行；提交后按产生顺序依次执行。
     */
    @Test
    void shouldDispatchOnlyAfterCommit() throws Exception {
        publisher = newPublisher();
        TransactionSynchronizationManager.initSynchronization();

        publisher.push(new TestFlowEvent(1, "process-a", false));
        publisher.push(new TestFlowEvent(2, "process-a", false));

        assertTrue(HANDLED.isEmpty(), "事务提交前不应投递事件");

        commit();

        awaitHandled(2);
        assertEquals(List.of(1, 2), HANDLED, "提交后应按产生顺序投递");
    }

    /**
     * 测试目标：事务回滚时事件不应投递。
     * 前置条件：存在活动事务同步。
     * 执行步骤：事务内推送事件 → 回滚（不触发 afterCommit）。
     * 期望断言：订阅方始终收不到事件。
     */
    @Test
    void shouldNotDispatchWhenTransactionRolledBack() throws Exception {
        publisher = newPublisher();
        TransactionSynchronizationManager.initSynchronization();

        publisher.push(new TestFlowEvent(1, "process-b", false));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        sleep(150);
        assertTrue(HANDLED.isEmpty(), "事务回滚后不应投递事件");
    }

    /**
     * 测试目标：同一流程（相同 eventKey）的事件严格按推送顺序投递，即使先推送的事件处理较慢。
     * 前置条件：存在活动事务同步，首个事件 handler 耗时 80ms。
     * 执行步骤：推送三个同 eventKey 事件 → 提交。
     * 期望断言：handler 执行顺序与推送顺序一致。
     */
    @Test
    void shouldKeepOrderForSameEventKey() throws Exception {
        publisher = newPublisher();
        slowEventId = 1;
        TransactionSynchronizationManager.initSynchronization();

        publisher.push(new TestFlowEvent(1, "process-c", false));
        publisher.push(new TestFlowEvent(2, "process-c", false));
        publisher.push(new TestFlowEvent(3, "process-c", false));

        commit();

        awaitHandled(3);
        assertEquals(List.of(1, 2, 3), HANDLED, "同一分区内必须按推送顺序执行");
    }

    /**
     * 测试目标：无事务上下文时（如定时任务、mock 调用）事件立即投递。
     * 前置条件：不存在活动事务同步。
     * 执行步骤：直接推送事件。
     * 期望断言：订阅方收到事件，且执行线程为事件投递线程。
     */
    @Test
    void shouldDispatchImmediatelyWithoutTransaction() throws Exception {
        publisher = newPublisher();

        publisher.push(new TestFlowEvent(1, "process-d", true));

        awaitHandled(1);
        assertEquals(List.of(1), HANDLED);
        assertTrue(HANDLED_THREADS.get(0).startsWith("flow-event-dispatch-"),
                "应在事件投递线程内执行，实际线程:" + HANDLED_THREADS.get(0));
    }

    /**
     * 测试目标：单个事件的 handler 异常不应阻塞同分区后续事件。
     * 前置条件：首个事件的 handler 抛出异常。
     * 执行步骤：推送两个同 eventKey 事件 → 提交。
     * 期望断言：第二个事件仍被正常处理。
     */
    @Test
    void shouldContinueDispatchWhenHandlerThrows() throws Exception {
        publisher = newPublisher();
        failEventId = 1;
        TransactionSynchronizationManager.initSynchronization();

        publisher.push(new TestFlowEvent(1, "process-e", false));
        publisher.push(new TestFlowEvent(2, "process-e", false));

        commit();

        awaitHandled(1);
        sleep(100);
        assertEquals(List.of(2), HANDLED, "handler 异常不应影响后续事件投递");
    }

    private SpringFlowEventPublisher newPublisher() throws Exception {
        SpringFlowEventPublisher eventPublisher = new SpringFlowEventPublisher(applicationContext, 1);
        eventPublisher.afterPropertiesSet();
        return eventPublisher;
    }

    private void commit() {
        List<TransactionSynchronization> synchronizations =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void awaitHandled(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && HANDLED.size() < expected) {
            sleep(10);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
