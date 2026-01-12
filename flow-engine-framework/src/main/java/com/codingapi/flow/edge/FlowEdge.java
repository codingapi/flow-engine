package com.codingapi.flow.edge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FlowEdge implements IFlowEdge{

    private String from;
    private String to;

    @Override
    public String from() {
        return from;
    }

    @Override
    public String to() {
        return to;
    }
}
