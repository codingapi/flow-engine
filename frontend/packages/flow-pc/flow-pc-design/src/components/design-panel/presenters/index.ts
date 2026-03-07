import {DesignPanelApi, FlowNode, initStateData, State, TabPanelType} from "../types";
import {Dispatch} from "@flow-engine/flow-core";
import {FormActionContext} from "@flow-engine/flow-types";
import {WorkflowFormManager} from "@/components/design-panel/manager/form";
import {NodeConvertorManager, NodeManger} from "@/components/design-panel/manager/node";
import {WorkflowConvertor} from "@/components/design-panel/presenters/convertor";

export class Presenter {

    private state: State;
    private readonly dispatch: Dispatch<State>;
    private readonly api: DesignPanelApi;
    private readonly formActionContext: FormActionContext;

    constructor(state: State, dispatch: Dispatch<State>, api: DesignPanelApi) {
        this.api = api;
        this.dispatch = dispatch;
        this.state = state;
        this.formActionContext = new FormActionContext();
    }


    public getFormActionContext() {
        return this.formActionContext;
    }

    /**
     * 手动同步节点数据
     * @param nodes
     */
    public syncNodes(nodes: any[]) {
        const nodeConvertorManager = new NodeConvertorManager();
        const data = nodeConvertorManager.toData(nodes);
        this.state = {
            ...this.state,
            workflow: {
                ...this.state.workflow,
                nodes: data,
            }
        }
    }

    public syncState(state: State) {
        this.state = state;
    }

    private mergeWorkflow(prevWorkflow: any, currentWorkflow: any) {
        const nodes: FlowNode[] = [];
        if (currentWorkflow.nodes && currentWorkflow.nodes.length > 0) {
            nodes.push(...currentWorkflow.nodes);
        } else {
            if (prevWorkflow.nodes && prevWorkflow.nodes.length > 0) {
                nodes.push(...prevWorkflow.nodes);
            }
        }
        return {
            ...prevWorkflow,
            ...currentWorkflow,
            form: {
                ...prevWorkflow.form,
                ...currentWorkflow.form,
            },
            nodes: nodes
        }
    }


    public updateViewPanelTab(tab: TabPanelType) {
        const values = this.formActionContext.save() as any;
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                view: {
                    ...prevState.view,
                    tabPanel: tab,
                },
                workflow: this.mergeWorkflow(prevState.workflow, values),
            }
        });
    }

    private updateWorkflowForm(form: any) {
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                workflow: {
                    ...prevState.workflow,
                    form: {
                        ...prevState.workflow.form,
                        ...form,
                    }
                }
            }
        });
    }

    public updateWorkflow(data: any) {
        this.dispatch((prevState: State) => {
            return {
                ...prevState,
                workflow: this.mergeWorkflow(prevState.workflow, data),
            }
        });
    }


    public removeWorkflowFormField(formCode: string, fieldCode: string) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.removeField(formCode, fieldCode);
        this.updateWorkflowForm(form);
    }

    public removeWorkflowSubForm(code: string) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.removeSubForm(code);
        this.updateWorkflowForm(form);
    }

    public addWorkflowSubForm(values: any) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.addSubForm(values);
        this.updateWorkflowForm(form);
    }


    public updateWorkflowFormField(code: string, values: any) {
        const workflowFormManager = new WorkflowFormManager(this.state.workflow.form);
        const form = workflowFormManager.updateFieldValue(code, values);
        this.updateWorkflowForm(form);
    }

    public async save() {
        const values = this.formActionContext.save() as any;
        this.updateWorkflow(values);
        const latest = {
            ...this.state,
            workflow: this.mergeWorkflow(this.state.workflow, values),
        };
        const convertor = new WorkflowConvertor(latest.workflow);
        const apiData = convertor.toApi();
        await this.api.save(apiData);
        console.log('save latest:', apiData);
    }

    public async createNode(form: string, type: string) {
        const flowNode = await this.api.createNode(type);
        const nodeManager = new NodeConvertorManager();
        const nodeManger = new NodeManger(this.state.workflow.nodes || []);
        const currentNode = nodeManger.getNode(form);
        const block = nodeManager.toItemRender(flowNode);
        if (currentNode) {
            if (currentNode.blocks) {
                const order = currentNode.blocks.length + 1;
                return {
                    ...block,
                    data: {
                        ...block.data,
                        order: order
                    }
                }
            }
        }

        return block;
    }


    public initState() {
        this.dispatch(initStateData);
    }

    public loadDesign(id: string) {
        this.api.load(id).then(result => {
            const convertor = new WorkflowConvertor(result);
            const renderData = convertor.toRender();
            this.updateWorkflow(renderData);
        });
    }

    public createDesign() {
        this.api.create().then(result => {
            const convertor = new WorkflowConvertor(result);
            const renderData = convertor.toRender();
            this.updateWorkflow(renderData);
        });
    }
}