import {FlowApprovalApi, State} from "@/components/flow-approval/typings";
import {FormActionContext} from "@/components/flow-approval/presenters/form";

export class FlowActionPresenter {

    private readonly api: FlowApprovalApi;
    private readonly formActionContext:FormActionContext;
    private state: State;

    constructor(state: State,
                api: FlowApprovalApi,
                formActionContext:FormActionContext) {
        this.state = state;
        this.api = api;
        this.formActionContext = formActionContext;
    }

    public syncState(state: State) {
        this.state = state;
    }

    public async processNodes(){
        const formData = this.formActionContext.save();
        const id = this.state.flow?.recordId || this.state.flow?.workId || '';
        return await this.api.processNodes({
            id,
            formData,
        });
    }


    public async action(actionId:string) {
        const recordId = this.state.flow?.recordId;
        const workId = this.state.flow?.workId;
        const formData = this.formActionContext.save();
        if(recordId){
            const request = {
                formData,
                recordId,
                advice:{
                    actionId,
                }
            }
            return await this.api.action(request);
        }else {
            const createRequest = {
                workId,
                formData,
                actionId,
            }
            const recordId = await this.api.create(createRequest);
            const actionRequest = {
                formData,
                recordId,
                advice:{
                    actionId,
                }
            }
            return await this.api.action(actionRequest);
        }
    }

}