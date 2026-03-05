import {FlowApprovalApi, State} from "@/components/flow-approval/typings";
import {FormActionContext} from "@flow-engine/flow-types";

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


    /**
     * 是否通过操作
     * @param actionId
     * @private
     */
    private isPassAction(actionId:string){
        const actions = this.state.flow?.actions || [];
        for(const action of actions){
            if(action.id === actionId){
                if(action.type ==='PASS'){
                    return true;
                }
            }
        }
        return false;
    }


    private async submitAction(actionId:string,formData:any){
        const recordId = this.state.flow?.recordId;
        const workId = this.state.flow?.workId;
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


    public async action(actionId:string) {
        let formData;
        if(this.isPassAction(actionId)){
            formData = await this.formActionContext.validate();
        }else {
            formData = this.formActionContext.save();
        }
        return await this.submitAction(actionId, formData);
    }

}