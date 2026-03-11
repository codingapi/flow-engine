import {DesignPanelApi, FlowNode, Workflow} from "./types";
import {create as workflowCreate,load as workflowLoad,save as workflowSave,createNode as workflowCreateNode} from "@/api/workflow";
import {result} from "lodash-es";

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


    public async createNode(type: string) {
        const data =  await workflowCreateNode(type);
        return data.data as FlowNode;
    }

}