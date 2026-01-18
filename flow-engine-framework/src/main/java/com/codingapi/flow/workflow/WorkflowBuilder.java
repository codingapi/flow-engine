package com.codingapi.flow.workflow;

import com.codingapi.flow.edge.FlowEdge;
import com.codingapi.flow.form.FormMeta;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.script.OperatorMatchScript;
import com.codingapi.flow.operator.IFlowOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程构建器
 */
public class WorkflowBuilder {

    private final Workflow workflow;

    private WorkflowBuilder() {
        this.workflow = new Workflow();
    }

    public static WorkflowBuilder builder() {
        return new WorkflowBuilder();
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

    public WorkflowBuilder revoke(boolean revoke) {
        workflow.setIsRevoke(revoke);
        return this;
    }

    public WorkflowBuilder interfere(boolean interfere) {
        workflow.setIsInterfere(interfere);
        return this;
    }

    public WorkflowBuilder createdOperator(IFlowOperator createdOperator) {
        workflow.setCreatedOperator(createdOperator);
        return this;
    }

    public WorkflowBuilder operatorCreateScript(String operatorCreateScript) {
        workflow.setOperatorCreateScript(new OperatorMatchScript(operatorCreateScript));
        return this;
    }

    public WorkflowBuilder addNode(IFlowNode node) {
        if (workflow.getNodes() == null) {
            workflow.setNodes(new ArrayList<>());
        }
        workflow.getNodes().add(node);
        return this;
    }

    public WorkflowBuilder nodes(List<IFlowNode> nodes) {
        workflow.setNodes(nodes);
        return this;
    }

    public WorkflowBuilder addEdge(FlowEdge edge) {
        if (workflow.getEdges() == null) {
            workflow.setEdges(new ArrayList<>());
        }
        workflow.getEdges().add(edge);
        return this;
    }

    public WorkflowBuilder edges(List<FlowEdge> edges) {
        workflow.setEdges(edges);
        return this;
    }

    public Workflow build() {
        workflow.verify();
        return workflow;
    }

}
