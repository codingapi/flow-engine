package com.codingapi.flow.strategy;

import java.util.Map;

public interface INodeStrategy {

    String TYPE_KEY = "strategyType";

    Map<String, Object> toMap();

    String getId();

    default String strategyType() {
        return this.getClass().getSimpleName();
    }

}
