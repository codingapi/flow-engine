package com.codingapi.flow.error;

import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 异常流程配置
 */
@Setter
@Getter
public class ErrorThrow {
    private IFlowNode node;
    private List<IFlowOperator> operators;

    public static Builder builder() {
        return new Builder();
    }

    public boolean isNode() {
        return node != null;
    }

    public static class Builder{
        private IFlowNode node;
        private List<IFlowOperator> operators;

        public Builder node(IFlowNode node) {
            this.node = node;
            return this;
        }

        public Builder operators(IFlowOperator... operators) {
            this.operators = List.of(operators);
            return this;
        }

        public Builder operators(List<IFlowOperator> operators) {
            this.operators = operators;
            return this;
        }

        public ErrorThrow build() {
            ErrorThrow errorThrow = new ErrorThrow();
            errorThrow.setNode(node);
            errorThrow.setOperators(operators);
            return errorThrow;
        }
    }
}
