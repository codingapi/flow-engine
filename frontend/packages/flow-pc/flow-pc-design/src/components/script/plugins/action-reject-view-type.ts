export const VIEW_KEY = 'ConditionRejectViewPlugin';

export interface ConditionRejectViewPlugin {
    // 当前节点id
    nodeId:string;
    // 当前的脚本
    value?: string;
    // 脚本更改回掉
    onChange?: (value: string) => void;
}