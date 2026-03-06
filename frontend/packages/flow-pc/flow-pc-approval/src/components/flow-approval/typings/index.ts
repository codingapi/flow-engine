import {FlowContent,ProcessNode} from "@flow-engine/flow-types";

/**
 * 流程审批布局组件属性
 */
export interface ApprovalLayoutProps {
    content:FlowContent;
    review?:boolean;
    onClose?:() => void;
}

// Layout constants
export const ApprovalLayoutHeight = 64;
export const ApprovalContentPaddingV = 24;
export const ApprovalContentPaddingH = 24;
export const ApprovalSidebarWidth = 250;
export const ApprovalSidebarCollapsedWidth = 48;


export type State  = {
    flow?:FlowContent;
    review?:boolean;
};


export const initStateData = {

}

export interface FlowApprovalApi{

    create(body:Record<string,any>):Promise<number>;

    processNodes(body:Record<string,any>):Promise<ProcessNode[]>;

    action(body:Record<string,any>):Promise<any>;
}
