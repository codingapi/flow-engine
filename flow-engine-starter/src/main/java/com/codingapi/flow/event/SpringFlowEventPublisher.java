package com.codingapi.flow.event;

import com.codingapi.springboot.framework.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流程事件发布器的 Spring 实现（issue #274）。
 *
 * <p>解决两个长期存在的一致性风险：</p>
 * <ol>
 *     <li><b>事务可见性</b>：有活动事务时，事件通过 {@link TransactionSynchronization#afterCommit()}
 *     在事务提交后才投递，订阅方回查数据库一定能读到流程记录/待办/关系数据；事务回滚则不投递。</li>
 *     <li><b>投递顺序</b>：按 {@link IFlowEvent#eventKey()}（默认流程id）散列到固定的单线程分区，
 *     同一流程的事件严格按产生顺序投递，不同流程可并行；投递时使用同步事件
 *     （{@code DomainEvent(sync=true)}）在分区线程内完成 handler 调用，避免再落入底层线程池导致乱序。</li>
 * </ol>
 *
 * <p>与底层 {@code EventPusher} 的关系：未注册本实现时（仅使用 framework、单元测试等场景），
 * {@link FlowEventPublisher} 直接回退 {@code EventPusher.push}，行为与旧版本一致。
 * handler 抛出异常时只记录日志，不阻塞同分区后续事件的投递。</p>
 */
@Slf4j
public class SpringFlowEventPublisher implements IFlowEventPublisher, InitializingBean, DisposableBean {

    /**
     * 默认分区数：分区内串行、分区间并行
     */
    private static final int DEFAULT_PARTITION_SIZE = 4;

    private final ApplicationContext applicationContext;

    private final ExecutorService[] partitions;

    public SpringFlowEventPublisher(ApplicationContext applicationContext) {
        this(applicationContext, DEFAULT_PARTITION_SIZE);
    }

    public SpringFlowEventPublisher(ApplicationContext applicationContext, int partitionSize) {
        this.applicationContext = applicationContext;
        int size = Math.max(1, partitionSize);
        this.partitions = new ExecutorService[size];
    }

    @Override
    public void afterPropertiesSet() {
        for (int i = 0; i < partitions.length; i++) {
            partitions[i] = Executors.newSingleThreadExecutor(new FlowEventThreadFactory(i));
        }
        FlowEventPublisher.register(this);
        log.info("SpringFlowEventPublisher success initial, partitions:{}", partitions.length);
    }

    @Override
    public void push(IFlowEvent event) {
        if (event == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(event);
                }
            });
            return;
        }
        dispatch(event);
    }

    /**
     * 提交事件到对应分区：分区的单线程执行器天然保证「先提交先执行」。
     */
    private void dispatch(IFlowEvent event) {
        String eventKey = event.eventKey();
        int index = eventKey == null ? 0 : Math.floorMod(eventKey.hashCode(), partitions.length);
        partitions[index].execute(() -> publish(event));
    }

    /**
     * 以同步事件方式投递，使 handler 在当前分区线程内执行完毕，从而获得确定顺序。
     */
    private void publish(IFlowEvent event) {
        try {
            applicationContext.publishEvent(new DomainEvent(event, true, generateTraceId()));
        } catch (Exception e) {
            log.error("flow event dispatch failed, eventType:{}, eventKey:{}",
                    event.getClass().getName(), safeEventKey(event), e);
        }
    }

    private String safeEventKey(IFlowEvent event) {
        try {
            return event.eventKey();
        } catch (Exception e) {
            return null;
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void destroy() {
        FlowEventPublisher.unregister(this);
        for (ExecutorService partition : partitions) {
            if (partition != null) {
                partition.shutdown();
            }
        }
    }

    private static class FlowEventThreadFactory implements ThreadFactory {

        private final int index;
        private final AtomicInteger counter = new AtomicInteger(1);

        private FlowEventThreadFactory(int index) {
            this.index = index;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "flow-event-dispatch-" + index + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
