import {ActionSelectOption} from "@/components/script/typings";

export const VIEW_KEY = 'ActionCustomViewPlugin';

export interface ActionCustomViewPlugin {
    // 当前的脚本
    value?: string;
    // 脚本更改回掉
    onChange?: (value: string) => void;
    // 可选择的动作范围
    options:ActionSelectOption[];
}