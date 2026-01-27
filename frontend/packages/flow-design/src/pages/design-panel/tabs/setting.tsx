import React from "react";
import {Panel} from "@/components/panel";
import {InterferePanel} from "@/pages/design-panel/panels/workflow/interfere";
import {UrgePanel} from "@/pages/design-panel/panels/workflow/urge";
import {CardForm} from "@/components/form/card";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";
import {WorkflowStrategyManager} from "@/pages/design-panel/manager/workflow/strategy";


export const TabSetting = () => {

    const [form] = CardForm.useForm();
    const {state, context} = useDesignContext();

    const formActionContext = context.getPresenter().getFormActionContext();


    React.useEffect(() => {
        form.resetFields();
        const workflowStrategyManager = new WorkflowStrategyManager(state.workflow.strategies);
        const formData = workflowStrategyManager.toForm();
        form.setFieldsValue(formData);
    }, []);

    React.useEffect(() => {
        const workflowStrategyManager = new WorkflowStrategyManager(state.workflow.strategies);
        const formData = workflowStrategyManager.toForm();
        form.setFieldsValue(formData);
    }, [state.workflow.strategies]);

    // 注册form行为
    React.useEffect(() => {
        formActionContext.addAction({
            save() {
                return form.getFieldsValue()
            }
        });
    }, []);

    return (
        <Panel>
            <InterferePanel form={form}/>
            <UrgePanel form={form}/>
        </Panel>
    )
}