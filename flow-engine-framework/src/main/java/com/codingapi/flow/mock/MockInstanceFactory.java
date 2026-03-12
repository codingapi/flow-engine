package com.codingapi.flow.mock;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.mock.service.FlowRecordQueryMockService;
import com.codingapi.flow.repository.WorkflowRepository;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟示例构建工厂
 */
@Slf4j
public class MockInstanceFactory {

    private final Map<String, MockInstance> cache;

    @Getter
    private final static MockInstanceFactory instance = new MockInstanceFactory();

    private MockInstanceFactory() {
        this.cache = new ConcurrentHashMap<>();
    }

    public MockInstance create(FlowOperatorGateway flowOperatorGateway, WorkflowRepository workflowRepository ) {
        String key = RandomUtils.generateStringId();
        MockRepositoryHolder mockRepositoryHolder = new MockRepositoryHolder(flowOperatorGateway,workflowRepository);
        FlowService flowService = new FlowService(mockRepositoryHolder);
        FlowRecordQueryMockService flowRecordQueryMockService = new FlowRecordQueryMockService(mockRepositoryHolder);
        MockInstance mockInstance = new MockInstance(key, mockRepositoryHolder, flowService, flowRecordQueryMockService);
        this.cache.put(key, mockInstance);
        log.info("create mock:{}",key);
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
        log.info("clear mock:{}",key);
    }

}
