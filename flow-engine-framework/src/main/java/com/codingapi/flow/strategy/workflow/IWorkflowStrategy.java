package com.codingapi.flow.strategy.workflow;

import java.util.Map;

/**
 *  工作流策略
 */
public interface IWorkflowStrategy {

    //todo toMap变成全局的接口
    Map<String,Object> toMap();
}
