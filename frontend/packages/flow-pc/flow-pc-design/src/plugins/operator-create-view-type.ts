export const VIEW_KEY = 'OperatorCreateViewPlugin';

export interface OperatorCreateViewPlugin {
    /** 当前脚本 */
    script: string;
    /** 确认回调 */
    onChange: (script: string) => void;
}