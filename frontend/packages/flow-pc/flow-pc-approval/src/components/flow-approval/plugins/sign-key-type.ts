import {FlowOperator} from "@flow-engine/flow-types";

export const VIEW_KEY = 'SignKeyViewPlugin';

export interface SignKeyViewPlugin {
    /** 当前用户 */
    current: FlowOperator;
    /** 返回签名 */
    onChange?: (value: string) => void;
    /** 当前签名 */
    value?: string;
}