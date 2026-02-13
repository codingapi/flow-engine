import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {BranchAdderRender} from "@/components/design-editor/components/branch-adder";

export const renderForm = ({form}: FormRenderProps<FlowNodeJSON['data']>) => {

    return (
        <BranchAdderRender
            buttonText={'添加并行分支'}
            addType={'PARALLEL_BRANCH'}
        />
    );
};


export const formMeta: FormMeta<FlowNodeJSON['data']> = {
  render: renderForm,
  validateTrigger: ValidateTrigger.onChange,
  validate: {
    title: ({ value }: { value: string }) => (value ? undefined : 'Title is required'),
  },
  effect: {
    title: syncVariableTitle,
    outputs: provideJsonSchemaOutputs,
  },
};
