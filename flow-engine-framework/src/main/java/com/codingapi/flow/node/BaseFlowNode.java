package com.codingapi.flow.node;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public abstract class BaseFlowNode implements IFlowNode {

    /**
     * 节点id
     */
    @Getter
    @Setter
    protected String id;
    /**
     * 节点名称
     */
    @Getter
    @Setter
    protected String name;



}
