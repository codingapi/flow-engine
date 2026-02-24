export interface FormFieldMeta{
    id:string;
    name:string;
    code:string;
    type:string;
    require:boolean;
    defaultValue:string;
}

export interface FlowMeta {
    name:string;
    code:string;
    fields:FormFieldMeta[];
    subForms:FlowMeta[];
}


export interface Body{
    recordId:number;
    title:string;
    data:Record<string,any>;
    recordState:number;
    flowState:number;
}

export interface ActionDisplay{
    title:string;
    style:string;
    icon:string;
}

export interface FlowAction{
    id:string;
    title:string;
    type:string;
    display:ActionDisplay;
    enable:boolean;
}

export interface FlowOperator {
    id:number;
    name:string;
}

export interface History{
    recordId:number;
    title:string;
    advice:string;
    signKey:string;
    nodeName:string;
    nodeId:string;
    nodeType:string
    updateTime:number;
}


export interface NextNode{
    nodeId:string;
    nodeName:string;
    nodeType:string;
    operators:FlowOperator[];
}

export interface FlowContent {
    recordId:number;
    workId:string;
    workCode:string;
    view:string;
    adviceNullable:boolean;
    signable:boolean;
    form:FlowMeta;
    todos:Body[];
    actions:FlowAction[];
    mergeable:boolean;
    createOperator:FlowOperator;
    currentOperator:FlowOperator;
    flowState:number;
    recordState:number;
    histories:History[];
    nextNodes:NextNode[];
}

export interface ApprovalLayoutProps {
    content:FlowContent;
    onClose?:() => void;
}


export type State  = {
    flow?:FlowContent;
};


export const initStateData = {

}

export interface FlowApprovalApi{

    create(body:Record<string,any>):Promise<number>;

    action(body:Record<string,any>):Promise<any>;
}
