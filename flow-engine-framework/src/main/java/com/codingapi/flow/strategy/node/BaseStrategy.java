package com.codingapi.flow.strategy.node;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.session.FlowSession;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础策略
 */
public abstract class BaseStrategy implements INodeStrategy {

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(TYPE_KEY, strategyType());
        return map;
    }

    @Override
    public void verifyNode(FlowForm form) {

    }

    @Override
    public void verifySession(FlowSession session) {

    }
}
