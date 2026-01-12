package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.IFlowEdge;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.permission.FormFieldPermission;
import com.codingapi.flow.node.EndNode;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.node.StartNode;
import com.codingapi.flow.script.OperatorMatchScript;
import com.codingapi.flow.user.IFlowOperator;
import com.codingapi.flow.utils.RandomUtils;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 流程对象
 */
@Data
public class Workflow {

    /**
     * 流程id
     */
    private String id;
    /**
     * 流程编号
     */
    private String code;
    /**
     * 流程名称
     */
    private String title;

    /**
     * 创建者
     */
    private IFlowOperator createdOperator;

    /**
     * 创建时间
     */
    private long createdTime;

    /**
     * 流程表单
     */
    private FlowForm form;

    /**
     * 创建者脚本
     */
    private OperatorMatchScript operatorCreateScript;

    /**
     * 流程节点
     */
    private List<IFlowNode> nodes;

    /**
     * 流程关系
     */
    private List<IFlowEdge> edges;


    public Workflow() {
        this.id = RandomUtils.generateStringId();
    }

    /**
     * 匹配创建者
     *
     * @param flowOperator 创建者
     * @return 是否匹配
     */
    public boolean matchCreatedOperator(IFlowOperator flowOperator) {
        return operatorCreateScript.match(flowOperator);
    }

    /**
     * 验证流程
     */
    public void verify() {
        this.verifyWorkflow();
        this.verifyNodes();
        this.verifyEdges();
    }

    private void verifyWorkflow() {
        if (!StringUtils.hasText(id)) {
            throw new IllegalArgumentException("workflow id can not be null");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("workflow code can not be null");
        }
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("workflow title can not be null");
        }
        if (form == null) {
            throw new IllegalArgumentException("workflow form can not be null");
        }
        if (createdOperator == null) {
            throw new IllegalArgumentException("workflow createdOperator can not be null");
        }
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("workflow nodes can not be null");
        }
        if (edges == null || edges.isEmpty()) {
            throw new IllegalArgumentException("workflow edges can not be null");
        }
    }


    private void verifyNodes() {
        int start = 0;
        int end = 0;
        for (IFlowNode node : nodes) {
            if (node instanceof StartNode) {
                start++;
            }
            if (node instanceof EndNode) {
                end++;
            }
        }
        if (start != 1 || end != 1) {
            throw new IllegalArgumentException("workflow nodes must have one start node and one end node");
        }

        for (IFlowNode node : nodes) {
            node.verify(form);
        }
    }

    private void verifyEdges() {
        for (IFlowEdge edge : edges) {
            if (edge.from().equals(edge.to())) {
                throw new IllegalArgumentException("workflow edges can not have same from and to");
            }
        }
        IFlowNode startNode = this.nodes.stream().filter(node -> node instanceof StartNode).findFirst().get();

        int startCount = 0;
        for (IFlowEdge edge : edges) {
            if (edge.from().equals(startNode.getId())) {
                startCount++;
            }
        }

        if (startCount != 1) {
            throw new IllegalArgumentException("workflow edges must have one start edge");
        }

        List<IFlowNode> nextNodes = next(startNode);
        for (IFlowNode nextNode : nextNodes) {
            this.verifyNextEdge(nextNode);
        }
    }

    private void verifyNextEdge(IFlowNode node) {
        if (node instanceof EndNode) {
            return;
        } else {
            List<IFlowNode> nextNodes = next(node);
            if (nextNodes.isEmpty()) {
                throw new IllegalArgumentException("workflow edges must have one end edge");
            }
            for (IFlowNode nextNode : nextNodes) {
                this.verifyNextEdge(nextNode);
            }
        }
    }


    public List<IFlowNode> next(IFlowNode node) {
        return edges.stream().filter(edge -> edge.from().equals(node.getId()))
                .map(edge -> nodes.stream().filter(node1 -> node1.getId().equals(edge.to())).findFirst().get()).toList();
    }

}
