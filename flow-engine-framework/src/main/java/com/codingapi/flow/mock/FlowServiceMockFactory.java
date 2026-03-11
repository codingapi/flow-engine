package com.codingapi.flow.mock;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.service.FlowService;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowServiceMockFactory {

    private final Map<String, FlowService> cache;

    @Getter
    private final static FlowServiceMockFactory instance = new FlowServiceMockFactory();

    private FlowServiceMockFactory() {
        this.cache = new ConcurrentHashMap<>();
    }

    public String create(FlowOperatorGateway flowOperatorGateway) {
        String key = RandomUtils.generateStringId();
        MockRepositoryHolder mockRepositoryHolder = new MockRepositoryHolder(key, flowOperatorGateway);
        FlowService flowService = new FlowService(mockRepositoryHolder);
        this.cache.put(key, flowService);
        return key;
    }

    public FlowService getFlowService(String key) {
        return this.cache.get(key);
    }

    public FlowQueryMockService getFlowQueryService(String key) {
        FlowService flowService = this.getFlowService(key);
        if (flowService != null) {
            MockRepositoryHolder mockRepositoryHolder = (MockRepositoryHolder) flowService.getRepositoryHolder();
            return new FlowQueryMockService(mockRepositoryHolder);
        }
        return null;
    }

    public void clear(String key) {
        this.cache.remove(key);
    }

}
