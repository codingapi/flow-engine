import {DesignPanelApi, Workflow} from "./types";
import {create as workflowCreate,load as workflowLoad,save as workflowSave} from "@/api/workflow";

export class DesignPanelApiImpl implements DesignPanelApi {

    public async create() {
        const result = await workflowCreate();
        return result.data as Workflow;
    }

    public async load(id: string) {
        const result = await workflowLoad(id);
        return result.data as Workflow;
    }

    public async save(body: any) {
        await workflowSave(body);
    }

}