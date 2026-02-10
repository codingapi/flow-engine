import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/editor/node-components/header";
import {NodePanel} from "@/components/editor/node-components/panel";
import {TabNodeLayout} from "@/components/editor/node-components/layout";
import {ErrorTriggerStrategy} from "@/components/editor/node-components/strategy/error-trigger";
import {NodeTitleStrategy} from "@/components/editor/node-components/strategy/node-title";
import {OperatorLoadStrategy} from "@/components/editor/node-components/strategy/operator-load";

export const renderForm = ({ form }: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel>
          <NodeHeader/>
          <TabNodeLayout hiddenAction={true}>
              <OperatorLoadStrategy/>
              <NodeTitleStrategy/>
              <ErrorTriggerStrategy/>
          </TabNodeLayout>
      </NodePanel>
    );
  }
  return (
    <NodePanel>
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
