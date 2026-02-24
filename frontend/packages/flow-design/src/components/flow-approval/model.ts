import {FlowApprovalApi} from "@/components/flow-approval/typings";
import {action as actionRecord, create as createRecord} from "@/api/record";

export class FlowApprovalApiImpl implements FlowApprovalApi {

    create = async (body: Record<string, any>)=> {
        const response = await createRecord(body);
        if(response.success){
            return response.data;
        }
    }

    action =async (body: Record<string, any>)=> {
        return await actionRecord(body);
    }

}