import {Workflow} from "@/components/design-panel/types";

export class WorkflowConvertor {

    private readonly workflow: Workflow;

    public constructor(workflow: Workflow) {
        this.workflow = workflow;
    }

    public toApi() {
        return this.workflow;
    }

    public toRender() {
        return this.workflow;
    }

}

