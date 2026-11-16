package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.IHandler;
import com.codingapi.springboot.framework.event.SpringDefaultEventHandler;
import com.codingapi.springboot.framework.properties.FrameworkProperties;
import com.codingapi.springboot.framework.properties.PropertiesContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事件发布器与底层事件框架（17.3.3 起默认 {@code @TransactionalEventListener(AFTER_COMMIT, fallbackExecution)}）
 * 的集成测试：使用真实 Spring 上下文与真实事务同步机制，验证
 * 「事务提交后才投递 + 同流程严格按顺序 + 回滚不投递 + 在事件分区线程内执行」。
 *
 * <p>与 {@link SpringFlowEventPublisherTest} 的区别：后者直接调用 handler，本测试经过
 * Spring 的事件多播与事务监听器装配，覆盖两套机制的交互。</p>
 */
class SpringFlowEventPublisherIntegrationTest {

    private static final List<Integer> HANDLED = new CopyOnWriteArrayList<>();
    private static final List<String> HANDLED_THREADS = new CopyOnWriteArrayList<>();

    /**
     * 上下文只创建一次：底层 ApplicationHandlerUtils 是静态注册表，反复创建上下文会重复注册 handler
     */
    private static AnnotationConfigApplicationContext context;
    private static PlatformTransactionManager transactionManager;

    private SpringFlowEventPublisher publisher;

    static class RecEvent implements IFlowEvent {

        private final int id;
        private final String eventKey;
        private final boolean mock;

        RecEvent(int id, String eventKey, boolean mock) {
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

    @Configuration
    static class TestConfig {

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new NoopTransactionManager();
        }

        @Bean
        public IHandler<RecEvent> recEventHandler() {
            return event -> {
                HANDLED.add(event.id);
                HANDLED_THREADS.add(Thread.currentThread().getName());
            };
        }

        @Bean
        public SpringDefaultEventHandler springDefaultEventHandler(List<IHandler> handlers) {
            return new SpringDefaultEventHandler(handlers);
        }
    }

    /**
     * 无实际资源的事务管理器：仅驱动 Spring 的事务同步（begin/commit/rollback）流程。
     */
    static class NoopTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @BeforeAll
    static void initContext() {
        PropertiesContext.getInstance().setProperties(new FrameworkProperties());
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        transactionManager = context.getBean(PlatformTransactionManager.class);
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void setUp() {
        HANDLED.clear();
        HANDLED_THREADS.clear();
        publisher = new SpringFlowEventPublisher(context, 1);
        publisher.afterPropertiesSet();
    }

    @AfterEach
    void tearDown() {
        if (publisher != null) {
            publisher.destroy();
        }
    }

    /**
     * 测试目标：事务内推送的事件在提交后才由订阅方处理，且同一流程（同 eventKey）严格按顺序。
     * 前置条件：真实 Spring 上下文 + 真实事务同步 + 事件发布器已注册。
     * 执行步骤：事务内连续推送两个事件 → 提交前观察 → 提交后观察。
     * 期望断言：提交前无 handler 执行；提交后按推送顺序执行，且执行线程为事件分区线程。
     */
    @Test
    void shouldDispatchAfterCommitInOrderThroughSpringListener() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.executeWithoutResult(status -> {
            publisher.push(new RecEvent(1, "process-integration", false));
            publisher.push(new RecEvent(2, "process-integration", false));
            assertTrue(HANDLED.isEmpty(), "事务提交前不应有 handler 执行");
        });

        awaitHandled(2);
        assertEquals(List.of(1, 2), HANDLED, "提交后应按推送顺序投递");
        assertTrue(HANDLED_THREADS.stream().allMatch(name -> name.startsWith("flow-event-dispatch-")),
                "handler 应在事件分区线程内执行，实际:" + HANDLED_THREADS);
    }

    /**
     * 测试目标：事务回滚时不投递任何事件。
     * 前置条件：同上。
     * 执行步骤：事务内推送事件后抛出异常触发回滚。
     * 期望断言：订阅方始终收不到事件。
     */
    @Test
    void shouldNotDispatchWhenTransactionRolledBack() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> template.executeWithoutResult(status -> {
            publisher.push(new RecEvent(3, "process-rollback", false));
            throw new IllegalStateException("rollback");
        }));

        Thread.sleep(300);
        assertTrue(HANDLED.isEmpty(), "事务回滚后不应投递事件");
    }

    /**
     * 测试目标：无事务时的事件立即投递（模拟 mock 模式 / 定时任务）。
     * 前置条件：不在事务上下文中。
     * 执行步骤：直接推送事件。
     * 期望断言：订阅方收到事件。
     */
    @Test
    void shouldDispatchImmediatelyWithoutTransaction() throws Exception {
        publisher.push(new RecEvent(4, "process-no-tx", true));

        awaitHandled(1);
        assertEquals(List.of(4), HANDLED);
    }

    private void awaitHandled(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && HANDLED.size() < expected) {
            Thread.sleep(10);
        }
    }
}
