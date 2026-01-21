package com.codingapi.flow.builder;

import com.codingapi.flow.action.IFlowAction;

import java.util.ArrayList;
import java.util.List;

public class ActionBuilder {

    private final List<IFlowAction> actions;

    private ActionBuilder() {
        this.actions = new ArrayList<>();
    }

    public static ActionBuilder builder() {
        return new ActionBuilder();
    }

    public ActionBuilder addAction(IFlowAction action) {
        this.actions.add(action);
        return this;
    }

    public List<IFlowAction> build() {
        return actions;
    }
}
