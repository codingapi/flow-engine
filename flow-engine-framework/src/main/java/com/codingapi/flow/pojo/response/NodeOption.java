package com.codingapi.flow.pojo.response;

import com.codingapi.flow.node.IDisplayNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodeOption {

    private String id;
    private String name;
    private String type;
    private boolean display;

    /**
     * 可选人员范围（发起人/审批人设定时返回）。
     * 为空表示不限范围，前端可选任意人；非空时前端弹窗展示并默认全选。
     */
    private List<IFlowOperator> operators;

    public NodeOption(IFlowNode node) {
        this(node, null);
    }

    public NodeOption(IFlowNode node, List<IFlowOperator> operators) {
        this.id = node.getId();
        this.name = node.getName();
        this.type = node.getType();
        this.display = node instanceof IDisplayNode;
        this.operators = operators;
    }
}
