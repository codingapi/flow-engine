package com.codingapi.flow.workflow;

import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.node.IFlowNode;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.node.OperatorMatchScript;
import com.codingapi.flow.strategy.workflow.IWorkflowStrategy;

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

    public WorkflowBuilder form(FlowForm form) {
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

    public WorkflowBuilder operatorCreateScript(String operatorCreateScript) {
        workflow.setOperatorCreateScript(new OperatorMatchScript(operatorCreateScript));
        return this;
    }

    public WorkflowBuilder strategies(List<IWorkflowStrategy> strategies) {
        workflow.setStrategies(strategies);
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


    public Workflow build(boolean enable) {
        if(enable) {
            workflow.enable();
        }
        return workflow;
    }

    public Workflow build() {
        return this.build(true);
    }

}
