package com.codingapi.flow.node;

import java.util.Map;

/**
 * 流程节点
 */
public interface IFlowNode {

    /**
     * 节点id
     */
    String getId();

    /**
     * 节点名称
     */
    String getName();

    /**
     * 流程类型
     */
    String getType();

    /**
     * 转化为map
     */
    Map<String, Object> toMap();



}
