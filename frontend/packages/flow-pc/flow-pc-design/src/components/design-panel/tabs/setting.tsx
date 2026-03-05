import React from "react";
import {CardForm, Panel} from "@flow-engine/flow-pc-ui";
import {InterferePanel} from "@/components/design-panel/panels/workflow/interfere";
import {UrgePanel} from "@/components/design-panel/panels/workflow/urge";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {WorkflowStrategyManager} from "@/components/design-panel/manager/strategy";


export const TabSetting = () => {

    const [form] = CardForm.useForm();
    const {state, context} = useDesignContext();

    const formActionContext = context.getPresenter().getFormActionContext();

    const workflowStrategyManager = new WorkflowStrategyManager();

    const resetFieldsValue = () => {
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
            save: () => {
                return workflowStrategyManager.toData(form.getFieldsValue());
            },
            key: () => {
                return 'setting';
            },
            validate: () => {
                return new Promise((resolve, reject) => {
                    form.validateFields()
                        .then(values => {
                            resolve(workflowStrategyManager.toData(values));
                        }).catch(reject)
                })
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