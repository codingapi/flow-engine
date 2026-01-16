package com.codingapi.flow.gateway.impl;

import com.codingapi.flow.gateway.FlowOperatorGateway;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.user.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserGateway implements FlowOperatorGateway {

    private final Map<Long, User> cache = new HashMap<>();


    @Override
    public IFlowOperator get(long id) {
        return cache.get(id);
    }

    @Override
    public List<IFlowOperator> findByIds(List<Long> ids) {
        return cache.values().stream().filter(user -> ids.contains(user.getUserId()))
                .map(user -> (IFlowOperator) user)
                .toList();
    }

    @Override
    public List<IFlowOperator> findByIds(long... ids) {
        List<Long> list = Arrays.stream(ids).boxed().toList();
        return this.findByIds(list);
    }

    public void save(User user) {
        cache.put(user.getUserId(), user);
    }
}
