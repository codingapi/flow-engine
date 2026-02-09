import React from "react";
import {Panel} from "@/components/panel";
import {InterferePanel} from "@/pages/design-panel/panels/workflow/interfere";
import {UrgePanel} from "@/pages/design-panel/panels/workflow/urge";
import {CardForm} from "@/components/form/card";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";
import {WorkflowStrategyManager} from "@/pages/design-panel/manager/strategy";


export const TabSetting = () => {

    const [form] = CardForm.useForm();
    const {state, context} = useDesignContext();

    const formActionContext = context.getPresenter().getFormActionContext();

    const workflowStrategyManager = new WorkflowStrategyManager();

    const resetFieldsValue = ()=>{
        const formData = workflowStrategyManager.toRender(state.workflow.strategies as any[]);
        form.setFieldsValue(formData);
    }

    React.useEffect(() => {
        resetFieldsValue();
    }, [state.workflow.strategies]);

    // 注册form行为
    React.useEffect(() => {
        form.resetFields();
        resetFieldsValue();

        formActionContext.addAction({
            save() {
                return workflowStrategyManager.toData(form.getFieldsValue());
            },
            key(): string {
                return 'setting';
            }
        });

        return () => {
            formActionContext.removeAction('setting');
        };
    }, []);

    return (
        <Panel>
            <InterferePanel form={form}/>
            <UrgePanel form={form}/>
        </Panel>
    )
}