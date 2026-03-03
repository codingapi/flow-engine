export const VIEW_KEY = 'OperatorLoadViewPlugin';

export interface OperatorLoadViewPlugin {
    /** 当前脚本 */
    script: string;
    /** 确认回调 */
    onChange: (script: string) => void;
}