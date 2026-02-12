package com.codingapi.flow.pojo.response;

import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  流程审批人
 */
@Data
@AllArgsConstructor
public class FlowOperator {

    private long id;
    private String name;

    public FlowOperator(IFlowOperator flowOperator) {
        this.id = flowOperator.getUserId();
        this.name = flowOperator.getName();
    }
}
