import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {TabNodeLayout} from "@/components/design-editor/node-components/layout";
import {ErrorTriggerStrategy} from "@/components/design-editor/node-components/strategy/error-trigger";
import {NodeTitleStrategy} from "@/components/design-editor/node-components/strategy/node-title";
import {OperatorLoadStrategy} from "@/components/design-editor/node-components/strategy/operator-load";
import {View} from "@/components/design-editor/node-components/view";

export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel data={data}>
          <NodeHeader/>
          <TabNodeLayout hiddenAction={true}>
              <View/>
              <OperatorLoadStrategy/>
              <NodeTitleStrategy/>
              <ErrorTriggerStrategy/>
          </TabNodeLayout>
      </NodePanel>
    );
  }
  return (
    <NodePanel data={data}>
        <NodeHeader/>
        notify
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
