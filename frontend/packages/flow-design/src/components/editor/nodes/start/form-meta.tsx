import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {Field, FieldRenderProps, FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON, JsonSchema} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {Input} from "antd";

export const renderForm = ({ form }: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <>
          sidebar start
          <Field
              name="value"
              render={({ field: { value, onChange } }: FieldRenderProps<JsonSchema>) => (
                  <Input value={value as any} onChange={onChange} />
              )}
          />
      </>
    );
  }
  return (
    <div>
       start
    </div>
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
