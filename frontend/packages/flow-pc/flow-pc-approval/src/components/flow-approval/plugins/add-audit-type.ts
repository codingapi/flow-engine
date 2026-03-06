export const VIEW_KEY = 'AddAuditViewPlugin';

export interface AddAuditViewPlugin {
    /** 返回用户 */
    onChange?: (value: string|string[]) => void;
    /** 当前用户 */
    value?: string|string[];
}