package com.codingapi.flow.transfer;

/**
 * 流程导入模式。
 */
public enum WorkflowImportMode {

    /**
     * 使用导入文件中的流程编码替换已存在的流程。
     */
    REPLACE,

    /**
     * 生成新的流程ID并创建独立流程；源流程编码未占用时保留，冲突时生成新编码。
     */
    INCREMENTAL
}
