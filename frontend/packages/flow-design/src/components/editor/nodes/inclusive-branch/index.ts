import {FlowNodeRegistry} from '../../typings';
import {formMeta} from './form-meta';

export const InclusiveBranchNodeRegistry: FlowNodeRegistry = {
    type: 'INCLUSIVE_BRANCH',
    extend: 'block',
    meta: {
        copyDisable: true,
        addDisable: true,
    },
    info: {
        icon: 'INCLUSIVE_BRANCH',
        description: '包容分支',
    },
    /**
     * Render node via formMeta
     */
    formMeta,
};
