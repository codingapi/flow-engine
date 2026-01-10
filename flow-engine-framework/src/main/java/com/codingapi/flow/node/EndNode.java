package com.codingapi.flow.node;

/**
 * 结束节点
 */
public class EndNode extends BaseNode  {

    public static final String NODE_TYPE = "end";


    @Override
    public String getType() {
        return NODE_TYPE;
    }
}
