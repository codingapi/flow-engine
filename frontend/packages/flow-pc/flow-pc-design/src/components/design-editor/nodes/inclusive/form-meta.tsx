import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {BranchAdderRender} from "@/components/design-editor/components/branch-adder";
import {NodePanel} from "@/components/design-editor/node-components/panel";


export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {

    return (
        <NodePanel data={data}>
            <BranchAdderRender
                buttonText={'添加包容分支'}
                addType={'INCLUSIVE_BRANCH'}
            />
        </NodePanel>
    );
};


export const formMeta: FormMeta<FlowNodeJSON['data']> = {
    render: renderForm,
    validateTrigger: ValidateTrigger.onChange,
    validate: {
        title: ({value}: { value: string }) => (value ? undefined : 'Title is required'),
    },
    effect: {
        title: syncVariableTitle,
        outputs: provideJsonSchemaOutputs,
    },
};
