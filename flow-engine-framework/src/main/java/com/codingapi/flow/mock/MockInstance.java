package com.codingapi.flow.mock;

import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.service.FlowService;
import lombok.Getter;

import java.util.Timer;
import java.util.TimerTask;


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

    private final Timer timer;

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
        this.timer = new Timer();
        this.createTime = System.currentTimeMillis();
        this.expiredTime = this.createTime + MAX_KEEP_TIME;
        this.initTimer();
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

    private void initTimer() {
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isExpired()) {
                    MockInstanceFactory.getInstance().clear(mockKey);
                    timer.cancel();
                    return;
                }
            }
        }, 0, 1000);
    }
}
