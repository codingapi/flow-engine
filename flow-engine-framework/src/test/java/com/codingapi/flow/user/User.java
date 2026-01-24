package com.codingapi.flow.user;

import com.codingapi.flow.operator.IFlowOperator;

public class User implements IFlowOperator {

    private final long userId;
    private final String name;
    private final boolean manager;
    private final User forwardOperator;

    public User(long userId, String name) {
        this(userId, name, false, null);
    }

    public User(long userId, String name, boolean manager) {
        this(userId, name, manager, null);
    }

    public User(long userId, String name, User forwardOperator) {
        this(userId, name, false, forwardOperator);
    }

    public User(long userId, String name, boolean manager, User forwardOperator) {
        this.userId = userId;
        this.name = name;
        this.manager = manager;
        this.forwardOperator = forwardOperator;
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
        return forwardOperator;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUserId() == userId;
    }
}
