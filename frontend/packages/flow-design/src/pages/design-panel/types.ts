// Tab布局类型
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
    open: boolean;
    onClose?: () => void;
}

// 表单字段
interface FormField{
    name: string;
    code: string;
    type: string;
    required: boolean;
    defaultValue: string;
}

// 流程表单
interface FlowForm {
    name:string;
    code:string;
    fields:FormField[];
    subForms:FlowForm[];
}
// 流程配置
interface Workflow {
    id: string;
    title: string;
    code: string;
    form: FlowForm;
    strategies:any[];
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
        form:{
            code:'',
            name:'',
            fields:[],
            subForms:[]
        },
        strategies:[]
    }
}


export interface DesignPanelApi {

}
