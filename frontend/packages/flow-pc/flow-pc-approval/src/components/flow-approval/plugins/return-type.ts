export const VIEW_KEY = 'ReturnViewPlugin';

export interface ReturnViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}