import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {Field, FieldRenderProps, FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON, JsonSchema} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {Input} from "antd";
import {NodeHeader} from "@/components/editor/node-components/header";
import {NodePanel} from "@/components/editor/node-components/panel";

export const renderForm = ({ form }: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel>
          <NodeHeader/>
          sidebar approval
          <Field
              name="value"
              render={({ field: { value, onChange } }: FieldRenderProps<JsonSchema>) => (
                  <Input value={value as any} onChange={onChange} />
              )}
          />
      </NodePanel>
    );
  }
  return (
    <NodePanel>
        <NodeHeader/>
        approval
    </NodePanel>
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
