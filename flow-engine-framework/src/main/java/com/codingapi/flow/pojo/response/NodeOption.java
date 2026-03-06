package com.codingapi.flow.pojo.response;

import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.IFlowNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NodeOption {

    private String id;
    private String name;
    private String type;
    private boolean display;

    public NodeOption(IFlowNode node) {
        this.id = node.getId();
        this.name = node.getName();
        this.type = node.getType();
        this.display = node instanceof IDisplayNode;
    }
}
