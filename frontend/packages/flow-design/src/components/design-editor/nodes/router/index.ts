import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';
import {nanoid} from "nanoid";

export const RouterNodeRegistry: FlowNodeRegistry = {
    type: 'ROUTER',
    extend: 'end',
    meta:{
        copyDisable: true,
        addDisable: false,
        expandable: false, // disable expanded
    },
    info: {
        icon: 'ROUTER',
        description: '路由节点',
    },
    canAdd(ctx, from) {
        // 上级节点是 CONDITION_BRANCH 则可以添加 ROUTER 节点
        while (from.parent) {
            if (from.parent.flowNodeType === 'CONDITION_BRANCH') {
                return true;
            }
            from = from.parent;
        }
        return false;
    },
    /**
     * Render node via formMeta
     */
    formMeta
};
