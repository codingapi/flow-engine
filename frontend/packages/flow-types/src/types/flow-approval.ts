/**
 *  数据类型
 */
export type DataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE';



// FormField字段类型
export const dataTypeOptions = [
    {
        label: '数字',
        value: 'NUMBER'
    },
    {
        label: '字符串',
        value: 'STRING'
    },
    {
        label: '日期类型',
        value: 'DATE'
    },
    {
        label: '布尔类型',
        value: 'BOOLEAN'
    },
]

/**
 *  流程表单字段元数据
 */
export interface FormField {
    id:string;
    name:string;
    code:string;
    type:DataType;
    required:boolean;
    defaultValue?:string;
    placeholder?:string;
    tooltip?:string;
    help?:string;
}

/**
 * 流程表单元数据
 */
export interface FlowForm {
    name:string;
    code:string;
    fields:FormField[];
    subForms:FlowForm[];
}

/**
 * 流程操作显示对象
 */
export interface FlowActionDisplay {
    title:string;
    style:string;
    icon:string;
}

/**
 * 流程操作对象
 */
export interface FlowAction{
    id:string;
    title:string;
    type:string;
    display:FlowActionDisplay;
    enable:boolean;
}

/**
 *  流程操作人对象
 */
export interface FlowOperator {
    id:number;
    name:string;
}

/**
 *  流程操作记录对象
 */
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

/**
 *  流程待办对象
 */
export interface FlowTodo {
    recordId:number;
    title:string;
    data:Record<string,any>;
    recordState:number;
    flowState:number;
}


/**
 *  流程审批人对象
 */
export interface FlowApprovalOperator {
    advice:string;
    signKey:string;
    approveTime:number;
    flowOperator:FlowOperator;
}

/**
 * 流程节点对象
 */
export interface ProcessNode{
    nodeId:string;
    nodeName:string;
    nodeType:string;
    state:number;
    operators:FlowApprovalOperator[]
}

/**
 * 流程审批内容对象
 */
export interface FlowContent {
    recordId:number;
    workId:string;
    workCode:string;
    view:string;
    adviceNullable:boolean;
    signable:boolean;
    form:FlowForm;
    todos:FlowTodo[];
    actions:FlowAction[];
    mergeable:boolean;
    createOperator:FlowOperator;
    currentOperator:FlowOperator;
    flowState:number;
    recordState:number;
    histories:History[];
}