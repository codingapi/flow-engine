package com.codingapi.flow.repository;

import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.user.User;

import java.util.HashMap;
import java.util.Map;

public class UserRepository implements FlowOperatorRepository {

    private final Map<Long, User> cache = new HashMap<>();

    @Override
    public IFlowOperator get(long id) {
        return cache.get(id);
    }

    public void save(User user) {
        cache.put(user.getUserId(), user);
    }
}
