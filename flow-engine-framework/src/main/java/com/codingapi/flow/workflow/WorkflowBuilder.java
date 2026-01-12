package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.IFlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.user.IFlowOperator;

import java.util.ArrayList;

/**
 * 流程构建器
 */
public class WorkflowBuilder {

    private final static WorkflowBuilder instance = new WorkflowBuilder();

    private Workflow workflow = new Workflow();

    private WorkflowBuilder() {
    }

    public static WorkflowBuilder builder() {
        return instance;
    }

    public WorkflowBuilder id(String id) {
        workflow.setId(id);
        return this;
    }

    public WorkflowBuilder code(String code) {
        workflow.setCode(code);
        return this;
    }

    public WorkflowBuilder form(FormMeta form) {
        workflow.setForm(form);
        return this;
    }

    public WorkflowBuilder title(String title) {
        workflow.setTitle(title);
        return this;
    }

    public WorkflowBuilder createdOperator(IFlowOperator createdOperator) {
        workflow.setCreatedOperator(createdOperator);
        return this;
    }

    public WorkflowBuilder addNode(IFlowNode node) {
        if (workflow.getNodes() == null) {
            workflow.setNodes(new ArrayList<>());
        }
        workflow.getNodes().add(node);
        return this;
    }

    public WorkflowBuilder addEdge(IFlowEdge edge) {
        if (workflow.getEdges() == null) {
            workflow.setEdges(new ArrayList<>());
        }
        workflow.getEdges().add(edge);
        return this;
    }

    public Workflow build() {
        workflow.verify();
        return workflow;
    }

}
