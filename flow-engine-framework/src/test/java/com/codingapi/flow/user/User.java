package com.codingapi.flow.user;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class User implements IFlowOperator {

    private final long userId;
    private final String name;

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
        return false;
    }

    @Override
    public IFlowOperator entrustOperator() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUserId() == userId;
    }
}
