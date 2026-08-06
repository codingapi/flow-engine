package com.codingapi.flow.mock;

import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.service.FlowService;
import lombok.Getter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 *  模拟实例对象，当MockInstance被创建以后若15分钟没人操作，则将会自动注销，当有人操作会持续延长15分钟的时间。
 */
public class MockInstance {

    @Getter
    private final String mockKey;
    @Getter
    private final MockRepositoryHolder repositoryHolder;
    @Getter
    private final FlowService flowService;
    @Getter
    private final FlowRecordQueryService flowRecordQueryService;

    /**
     * 共享清理调度线程，替代每个实例一个 Timer 原生线程。
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "mock-instance-clear");
                thread.setDaemon(true);
                return thread;
            });

    private final ScheduledFuture<?> timerFuture;

    @Getter
    private final long createTime;

    @Getter
    private long expiredTime;

    // 最大活跃时长，15分钟
    private static final long MAX_KEEP_TIME = 1000 * 60 * 15;

    public MockInstance(String mockKey, MockRepositoryHolder repositoryHolder, FlowService flowService, FlowRecordQueryService flowRecordQueryService) {
        this.mockKey = mockKey;
        this.repositoryHolder = repositoryHolder;
        this.flowService = flowService;
        this.flowRecordQueryService = flowRecordQueryService;
        this.createTime = System.currentTimeMillis();
        this.expiredTime = this.createTime + MAX_KEEP_TIME;
        this.timerFuture = SCHEDULER.scheduleAtFixedRate(() -> {
            if (isExpired()) {
                MockInstanceFactory.getInstance().clear(mockKey);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 取消定时清理任务（外部主动清理时调用）。
     */
    public void cancel() {
        this.timerFuture.cancel(false);
    }

    public void updateExpiredTime(){
        this.expiredTime = System.currentTimeMillis() + MAX_KEEP_TIME;
    }

    /**
     * 是否到期
     */
    public boolean isExpired(){
        return System.currentTimeMillis() > expiredTime;
    }
}
