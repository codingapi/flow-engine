import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/editor/node-components/header";
import {NodePanel} from "@/components/editor/node-components/panel";
import {TabNodeLayout} from "@/components/editor/node-components/layout";
import {OperatorLoadStrategy} from "@/components/editor/node-components/strategy/operator-load";
import {NodeTitleStrategy} from "@/components/editor/node-components/strategy/node-title";
import {MultiOperatorAuditStrategy} from "@/components/editor/node-components/strategy/multi-operator-audit";
import {SameOperatorAuditStrategy} from "@/components/editor/node-components/strategy/same-operator-audit";
import {ErrorTriggerStrategy} from "@/components/editor/node-components/strategy/error-trigger";
import {ResubmitStrategy} from "@/components/editor/node-components/strategy/resubmit";
import {AdviceStrategy} from "@/components/editor/node-components/strategy/advice";
import {TimeoutStrategy} from "@/components/editor/node-components/strategy/timeout";
import {RecordMergeStrategy} from "@/components/editor/node-components/strategy/record-merge";
import {RevokeStrategy} from "@/components/editor/node-components/strategy/revoke";
import {View} from "@/components/editor/node-components/view";

export const renderForm = ({ form }: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel>
          <NodeHeader/>
          <TabNodeLayout>
              <View/>
              <OperatorLoadStrategy/>
              <NodeTitleStrategy/>
              <MultiOperatorAuditStrategy/>
              <SameOperatorAuditStrategy/>
              <ErrorTriggerStrategy/>
              <ResubmitStrategy/>
              <AdviceStrategy/>
              <TimeoutStrategy/>
              <RecordMergeStrategy/>
              <RevokeStrategy/>
          </TabNodeLayout>
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
