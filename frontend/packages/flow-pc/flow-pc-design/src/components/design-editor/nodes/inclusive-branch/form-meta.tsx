import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {ConditionScript} from "@/components/design-editor/node-components/condition";
import React from "react";
import {NodeHint} from "@/components/design-editor/node-components/node-hint";
import {NodeOrder} from "@/components/design-editor/node-components/node-order";

export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {
    const isSidebar = useIsSidebar();
    if (isSidebar) {
        return (
            <NodePanel data={data}>
                <NodeHeader/>
                <NodeOrder/>
                <ConditionScript/>
            </NodePanel>
        );
    }
    return (
        <NodePanel data={data}>
            <NodeHeader/>
            <NodeHint fieldName={"script"}/>
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
