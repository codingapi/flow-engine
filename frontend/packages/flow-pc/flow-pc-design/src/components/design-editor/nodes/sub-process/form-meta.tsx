import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';

import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {PanelLayout} from "@/components/design-editor/node-components/layout";
import {SubProcessStrategy} from "@/components/design-editor/node-components/strategy/sub-process";
import {GroovyScriptConvertorUtil} from "@/components/script/utils/convertor";

export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {
    const isSidebar = useIsSidebar();
    const script = data.form.getValueIn('SubProcessStrategy.script');
    if (isSidebar) {
        return (
            <NodePanel data={data}>
                <NodeHeader/>
                <PanelLayout>
                    <SubProcessStrategy/>
                </PanelLayout>
            </NodePanel>
        );
    }
    return (
        <NodePanel data={data}>
            <NodeHeader/>
            {GroovyScriptConvertorUtil.getScriptTitle(script)}
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
