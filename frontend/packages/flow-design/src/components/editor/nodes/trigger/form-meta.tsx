import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/editor/node-components/header";
import {NodePanel} from "@/components/editor/node-components/panel";
import {PanelLayout} from "@/components/editor/node-components/layout";
import {TriggerStrategy} from "@/components/editor/node-components/strategy/trigger";

export const renderForm = ({ form }: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel>
          <NodeHeader/>
          <PanelLayout>
              <TriggerStrategy/>
          </PanelLayout>
      </NodePanel>
    );
  }
  return (
    <NodePanel>
        <NodeHeader/>
        trigger
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
