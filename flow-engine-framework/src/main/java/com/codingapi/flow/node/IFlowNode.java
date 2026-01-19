package com.codingapi.flow.node;

import com.codingapi.flow.form.FormMeta;

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

    /**
     * 节点验证
     */
    void verify(FormMeta form);



}
