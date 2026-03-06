import {provideJsonSchemaOutputs, syncVariableTitle,} from '@flowgram.ai/form-materials';
import {FormMeta, FormRenderProps, ValidateTrigger,} from '@flowgram.ai/fixed-layout-editor';
import {FlowNodeJSON} from '../../typings';
import {useIsSidebar} from '../../hooks';
import {NodeHeader} from "@/components/design-editor/node-components/header";
import {NodePanel} from "@/components/design-editor/node-components/panel";
import {TabNodeLayout} from "@/components/design-editor/node-components/layout";
import {NodeTitleStrategy} from "@/components/design-editor/node-components/strategy/node-title";
import {RevokeStrategy} from "@/components/design-editor/node-components/strategy/revoke";
import {View} from "@/components/design-editor/node-components/view";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

export const renderForm = (data: FormRenderProps<FlowNodeJSON['data']>) => {
    const isSidebar = useIsSidebar();
    const {state} = useDesignContext();
    if (isSidebar) {
        return (
            <NodePanel data={data}>
                <NodeHeader/>
                <TabNodeLayout>
                    <View/>
                    <NodeTitleStrategy/>
                    <RevokeStrategy/>
                </TabNodeLayout>
            </NodePanel>
        );
    }
    return (
        <NodePanel data={data}>
            <NodeHeader/>
            {GroovyScriptConvertorUtil.getScriptTitle(state.workflow.operatorCreateScript)}
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
