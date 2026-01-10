package com.codingapi.flow.edge;

/**
 * 边接口
 */
public interface IFlowEdge {

    /**
     * 边id
     */
    String getId();

    /**
     * 边源节点
     */
    String from();

    /**
     * 边目标节点
     */
    String to();
}
