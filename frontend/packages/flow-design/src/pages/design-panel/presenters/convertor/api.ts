import {Workflow} from "@/pages/design-panel/types";
import {WorkflowNodeConvertor} from "@/pages/design-panel/presenters/convertor/node";
import {WorkflowEdgeConvertor} from "@/pages/design-panel/presenters/convertor/edge";

export class WorkflowApiConvertor {

    private readonly workflow: Workflow;
    private readonly workflowNodeConvertor:WorkflowNodeConvertor;
    private readonly workflowEdgeConvertor:WorkflowEdgeConvertor;

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
        this.workflowNodeConvertor = new WorkflowNodeConvertor(workflow);
        this.workflowEdgeConvertor = new WorkflowEdgeConvertor(workflow);
    }

    public toApi() {
        return {
            ...this.workflow,
            edges: this.workflowEdgeConvertor.toEdges(),
            nodes: this.workflowNodeConvertor.toNodes(),
        };
    }

}

