package com.codingapi.flow.node;

/**
 *  插槽节点 包括 {@link com.codingapi.flow.node.nodes.ConditionNode}{@link com.codingapi.flow.node.nodes.InclusiveNode} {@link com.codingapi.flow.node.nodes.ParallelNode}
 */
public interface IBlockNode {

    /**
     * 添加默认的branch分支
     * @param count 默认数量
     */
    void addDefaultBranch(int count);

}
