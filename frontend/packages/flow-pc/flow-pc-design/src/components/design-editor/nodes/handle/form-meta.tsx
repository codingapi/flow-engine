import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {TabNodeLayout} from "@/components/design-editor/node-components/layout";
import {OperatorLoadStrategy} from "@/components/design-editor/node-components/strategy/operator-load";
import {NodeTitleStrategy} from "@/components/design-editor/node-components/strategy/node-title";
import {ErrorTriggerStrategy} from "@/components/design-editor/node-components/strategy/error-trigger";
import {TimeoutStrategy} from "@/components/design-editor/node-components/strategy/timeout";
import {MultiOperatorAuditStrategy} from "@/components/design-editor/node-components/strategy/multi-operator-audit";
import {SameOperatorAuditStrategy} from "@/components/design-editor/node-components/strategy/same-operator-audit";
import {RecordMergeStrategy} from "@/components/design-editor/node-components/strategy/record-merge";
import {ResubmitStrategy} from "@/components/design-editor/node-components/strategy/resubmit";
import {AdviceStrategy} from "@/components/design-editor/node-components/strategy/advice";
import {View} from "@/components/design-editor/node-components/view";
import {CurrentNodeOperator} from "@/components/design-editor/node-components/current-operator";
import React from "react";

export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {
  const isSidebar = useIsSidebar();
  if (isSidebar) {
    return (
      <NodePanel data={data}>
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
          </TabNodeLayout>
      </NodePanel>
    );
  }
  return (
    <NodePanel data={data}>
        <NodeHeader/>
        <CurrentNodeOperator/>
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
