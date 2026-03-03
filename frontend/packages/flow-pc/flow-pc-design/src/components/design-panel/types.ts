// Tab布局类型
import {NodeType} from "@/components/design-editor/typings/node-type";

export type TabPanelType = 'base' | 'form' | 'flow' | 'setting';

// 布局顶部高度
export const LayoutHeaderHeight = 50;

// Form字段类型
export const FormFieldOptions = [
        {
            label: '整数',
            value: 'number'
        },
        {
            label: '字符串',
            value: 'string'
        },
        {
            label: '小数类型',
            value: 'float'
        },
        {
            label: '布尔类型',
            value: 'boolean'
        },
]


export interface DesignPanelProps {
    // 流程编码
    id?:string
    open: boolean;
    onClose?: () => void;
}

// 表单字段
export interface FormField{
    id:string;
    name: string;
    code: string;
    type: string;
    required: boolean;
    defaultValue: string;
}

// 流程表单
export interface FlowForm {
    name:string;
    code:string;
    fields:FormField[];
    subForms:FlowForm[];
}


// 流程配置
export interface Workflow {
    id: string;
    title: string;
    code: string;
    form: FlowForm;
    // 流程创建人脚本
    operatorCreateScript:string;
    strategies?:any[];
    nodes?:FlowNode[];
    edges?:FlowEdge[];
}

// 节点关系
export interface FlowEdge {
    from:string;
    to:string;
}

// 动作展示
export interface ActionDisplay{
    title:string;
    style:any;
    icon:string;
}

// 节点动作
export interface FlowAction{
    id:string;
    type:string;
    title:string;
    display:ActionDisplay;
}

// 流程节点
export interface FlowNode{
    id:string;
    name:string;
    type:NodeType;
    order:number;
    actions:FlowAction[];
    strategies:any[];
    blocks?:FlowNode[];
    script?:string;
    view?:string;
}

// 全局状态
export interface State {
    view:{
        tabPanel: TabPanelType;
    },
    workflow:Workflow
}

// 初始化数据
export const initStateData: State = {
    view:{
        tabPanel:'base'
    },
    workflow:{
        id:'',
        title:'',
        code:'',
        operatorCreateScript:'',
        form:{
            code:'',
            name:'',
            fields:[],
            subForms:[]
        }
    }
}


export interface DesignPanelApi {

    create():Promise<Workflow>;

    load(id:string): Promise<Workflow>;

    save(body:any): Promise<void>;

    createNode(type:string):Promise<FlowNode>;
}
