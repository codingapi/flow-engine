package com.codingapi.flow.strategy.workflow;

import java.util.HashMap;
import java.util.Map;

public class BaseStrategy implements IWorkflowStrategy{

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>();
    }
}
