package com.codingapi.flow.node.error;

/**
 * 异常流程配置
 */
public class ErrorThrow {

    public boolean isNode(){
        return this instanceof NodeThrow;
    }

    public boolean isOperator(){
        return this instanceof OperatorThrow;
    }

}
