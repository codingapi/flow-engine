package com.codingapi.flow.pojo.response;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import lombok.Data;

/**
 *  流程审批人
 *
 *  <p>响应体为公开契约：保留 (id, name) 双参构造器保证下游源码兼容，
 *  身份标识通过新增构造器或 setter 补充（issue #288）。
 */
@Data
public class FlowOperator {

    private long id;
    private String name;

    /**
     * 身份标识，空串表示默认身份
     */
    private String identity;

    /**
     * 兼容历史调用方的双参构造器，身份为默认身份
     */
    public FlowOperator(long id, String name) {
        this(id, name, OperatorIdentity.DEFAULT);
    }

    public FlowOperator(long id, String name, String identity) {
        this.id = id;
        this.name = name;
        this.identity = OperatorIdentity.normalize(identity);
    }

    public FlowOperator(IFlowOperator flowOperator) {
        this.id = flowOperator.getUserId();
        this.name = flowOperator.getName();
        this.identity = OperatorIdentity.of(flowOperator);
    }
}
