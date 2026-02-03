import {Workflow} from "@/pages/design-panel/types";
import {WorkflowApiConvertor} from "./api";
import {WorkflowRenderConvertor} from "./render";

export class WorkflowConvertor {

    private readonly workflowApiConvertor: WorkflowApiConvertor;
    private readonly workflowRenderConvertor: WorkflowRenderConvertor;

    public constructor(workflow: Workflow) {
        this.workflowApiConvertor = new WorkflowApiConvertor(workflow);
        this.workflowRenderConvertor = new WorkflowRenderConvertor(workflow);
    }

    public toApi() {
        return this.workflowApiConvertor.toApi();
    }

    public toRender() {
        return this.workflowRenderConvertor.toRender();
    }

}

