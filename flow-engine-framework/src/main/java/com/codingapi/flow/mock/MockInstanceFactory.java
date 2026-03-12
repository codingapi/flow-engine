package com.codingapi.flow.mock;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.mock.server.FlowRecordQueryMockService;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockInstanceFactory {

    private final Map<String, MockInstance> cache;

    @Getter
    private final static MockInstanceFactory instance = new MockInstanceFactory();

    private MockInstanceFactory() {
        this.cache = new ConcurrentHashMap<>();
    }

    public MockInstance create(FlowOperatorGateway flowOperatorGateway) {
        String key = RandomUtils.generateStringId();
        MockRepositoryHolder mockRepositoryHolder = new MockRepositoryHolder(flowOperatorGateway);
        FlowService flowService = new FlowService(mockRepositoryHolder);
        FlowRecordQueryMockService flowRecordQueryMockService = new FlowRecordQueryMockService(mockRepositoryHolder);
        MockInstance mockInstance = new MockInstance(key, mockRepositoryHolder, flowService, flowRecordQueryMockService);
        this.cache.put(key, mockInstance);
        return mockInstance;
    }

    public MockInstance getMockInstance(String key) {
        MockInstance mockInstance = this.cache.get(key);
        if (mockInstance != null) {
            mockInstance.updateExpiredTime();
        }
        return mockInstance;
    }


    public void clear(String key) {
        this.cache.remove(key);
    }

}
