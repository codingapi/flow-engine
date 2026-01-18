package com.codingapi.flow.strategy;

import java.util.Map;

public interface INodeStrategy {

    String TYPE_KEY = "strategyType";

    Map<String, Object> toMap();

    default String strategyType() {
        return this.getClass().getSimpleName();
    }

}
