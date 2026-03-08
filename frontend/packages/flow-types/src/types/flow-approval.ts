import {ActionType} from "@/types/flow-design";

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
    // 字段id
    id: string;
    // 字段名称
    name: string;
    // 字段编码
    code: string;
    // 数据类型
    type: DataType;
    // 是否必填
    required: boolean;
    // 默认值
    defaultValue?: string;
    // 输入提示
    placeholder?: string;
    // 提醒提示
    tooltip?: string;
    // 帮助提示
    help?: string;
}

/**
 * 流程表单元数据
 */
export interface FlowForm {
    // 表单名称
    name: string;
    // 表单编码
    code: string;
    // 表单字段
    fields: FormField[];
    // 子表单
    subForms: FlowForm[];
}

/**
 *  展示样式
 */
export interface DisplayStyle {
    // 边框颜色
    borderColor?: string;
    // 背景颜色
    backgroundColor?: string;
    // 边框大小
    borderSize?: string;
    // 边框圆角
    borderRadius?: string;
}

/**
 * 流程操作显示对象
 */
export interface FlowActionDisplay {
    // 标题
    title: string;
    // 样式
    style: string;
    // 图标
    icon: string;
}

/**
 * 流程操作对象
 */
export interface FlowAction {
    // 操作id
    id: string;
    // 按钮名称
    title: string;
    // 动作类型
    type: ActionType;
    // 展示样式
    display: FlowActionDisplay;
    // 是否启用
    enable: boolean;
    // 自定义脚本
    script?: string;
}

/**
 *  流程操作人对象
 */
export interface FlowOperator {
    // 人员id
    id: number;
    // 人员名称
    name: string;
}

/**
 *  流程操作记录对象
 */
export interface History {
    // 记录id
    recordId: number;
    // 流程标题
    title: string;
    // 审批意见
    advice: string;
    // 审批签名key
    signKey: string;
    // 审批节点
    nodeName: string;
    // 审批节点id
    nodeId: string;
    // 节点类型
    nodeType: string
    // 审批时间
    updateTime: number;
}

/**
 *  流程待办对象
 */
export interface FlowTodo {
    // 记录id
    recordId: number;
    // 流程标题
    title: string;
    // 流程数据
    data: Record<string, any>;
    // 记录状态
    recordState: number;
    // 流程状态
    flowState: number;
}


/**
 *  流程审批人对象
 */
export interface FlowApprovalOperator {
    // 意见信息
    advice: string;
    // 签名key
    signKey: string;
    // 审核时间
    approveTime: number;
    // 审批动作
    actionName: string;
    // 审批人
    flowOperator: FlowOperator;
}

/**
 * 流程节点对象
 */
export interface ProcessNode {
    // 节点id
    nodeId: string;
    // 节点名称
    nodeName: string;
    // 节点类型
    nodeType: string;
    // 状态 -1 历史 0 当前 1 未执行
    state: number;
    // 审批人员
    operators: FlowApprovalOperator[]
}

/**
 * 节点
 */
export interface NodeOption {
    // 节点id
    id: string;
    // 节点名称
    name: string;
    // 节点类型
    type: string;
    // 展示节点
    display: boolean;
}

/**
 * 流程审批内容对象
 */
export interface FlowContent {
    // 记录id
    recordId: number;
    // 流程id
    processId: string;
    // 流程设计id
    workId: string;
    // 流程设计编码
    workCode: string;
    // 节点id
    nodeId: string;
    // 节点名称
    nodeName: string;
    // 节点类型
    nodeType: string;
    // 流程标题
    title: string;
    // 视图名称
    view: string;
    // 是否必填意见
    adviceRequired: boolean;
    // 是否必填签名
    signRequired: boolean;
    // 表单元数据
    form: FlowForm;
    // 待办记录
    todos: FlowTodo[];
    // 操作按钮
    actions: FlowAction[];
    // 是否合并
    mergeable: boolean;
    // 流程创建人
    createOperator: FlowOperator;
    // 当前流程
    currentOperator: FlowOperator;
    // 流程状态
    flowState: number;
    // 记录状态
    recordState: number;
    // 审批记录
    histories: History[];
    // 所有节点
    nodes: NodeOption[];
}