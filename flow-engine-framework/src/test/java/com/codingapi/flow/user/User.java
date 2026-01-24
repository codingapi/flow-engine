package com.codingapi.flow.user;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;

public class User implements IFlowOperator {

    private final long userId;
    private final String name;
    private final boolean manager;

    public User(long userId, String name) {
        this(userId, name, false);
    }

    public User(long userId, String name, boolean manager) {
        this.userId = userId;
        this.name = name;
        this.manager = manager;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFlowManager() {
        return manager;
    }

    @Override
    public IFlowOperator forwardOperator() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUserId() == userId;
    }
}
