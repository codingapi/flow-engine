import {GroovyVariableMapping, ScriptType} from "@/components/script/typings";

export const VIEW_KEY = 'ErrorTriggerViewPlugin';

export interface ErrorTriggerViewPlugin {
    /** 脚本类型 */
    type: ScriptType;
    /** 当前脚本 */
    script: string;
    /** 变量映射列表 */
    variables: GroovyVariableMapping[];
    /** 确认回调 */
    onChange: (script: string) => void;
}