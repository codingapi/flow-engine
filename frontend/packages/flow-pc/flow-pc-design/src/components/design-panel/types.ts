// Tab布局类型
import {FlowForm,NodeType,FlowAction} from "@flow-engine/flow-types";

export type TabPanelType = 'base' | 'form' | 'flow' | 'setting';

// 布局顶部高度
export const LayoutHeaderHeight = 50;


export interface DesignPanelProps {
    // 流程编码
    id?:string
    // 是否开启
    open: boolean;
    // 关闭
    onClose?: () => void;
}


// 流程配置
export interface Workflow {
    // 设计id
    id: string;
    // 流程名称
    title: string;
    // 流程编码
    code: string;
    // 流程表单
    form: FlowForm;
    // 流程创建人脚本
    operatorCreateScript:string;
    // 流程策略
    strategies?:any[];
    // 流程节点
    nodes?:FlowNode[];
}


// 流程节点
export interface FlowNode{
    // 节点id
    id:string;
    // 节点名称
    name:string;
    // 节点类型
    type:NodeType;
    // 节点优先级
    order:number;
    // 节点动作
    actions:FlowAction[];
    // 节点策略
    strategies:any[];
    // 节点条件块
    blocks?:FlowNode[];
    // 节点表达式
    script?:string;
    // 节点视图
    view?:string;
    // 流程展示节点
    display?:boolean;
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
