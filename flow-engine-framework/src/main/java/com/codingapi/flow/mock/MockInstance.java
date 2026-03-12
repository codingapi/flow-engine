package com.codingapi.flow.mock;

import com.codingapi.flow.query.FlowRecordQueryService;
import com.codingapi.flow.service.FlowService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;


@Slf4j
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

    private final long createTime;

    private long expiredTime;

    private static final long MAX_KEEP_TIME = 1000 * 60 * 30;

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

    private void initTimer() {
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (expiredTime > currentTime) {
                    MockInstanceFactory.getInstance().clear(mockKey);
                    timer.cancel();
                    return;
                }
            }
        }, 0, 1000);
    }
}
