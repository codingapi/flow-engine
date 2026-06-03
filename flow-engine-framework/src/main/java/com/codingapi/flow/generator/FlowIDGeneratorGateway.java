package com.codingapi.flow.generator;

/**
 * 流程id构建器
 */
public interface FlowIDGeneratorGateway {

    /**
     * 构建流程id
     */
    String generateWorkId();

    /**
     * 构建流程编码
     */
    String generateWorkCode();

    /**
     * 构建processId
     */
    String generateProcessId();

    /**
     * 构建动作id
     */
    String generateActionId();

    /**
     * 构建视图代码
     */
    String generateViewCode();

    /**
     * 构建节点id
     */
    String generateNodeId();

    /**
     * 构建并行id
     */
    String generateParallelId();

    /**
     * 构建延迟任务Id
     */
    String generateDelayTaskId();

    /**
     * 构建表单字段id
     */
    String generateFormFieldId();

    /**
     * 构建模拟key
     */
    String generateMockKey();

    /**
     * 构建脚本key
     */
    String generateFlowScriptKey();

    /**
     * 构建流程记录id
     */
    long generateRecordId();

}
