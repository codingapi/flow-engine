import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Col, Form, Row} from "antd";
import {ViewPlugin} from "@/plugins/view";

interface FormViewComponentProps{
    onValuesChange?:(values:any)=>void;
}

const FormViewComponent: React.FC<FormViewComponentProps> = (props) => {
    const {state, context} = useApprovalContext();
    const ViewComponent = ViewPlugin.getInstance().get(state.flow?.view || 'default');
    // 是否可合并审批
    const mergeable = state.flow?.mergeable || false;
    const todos = state.flow?.todos || [];
    const viewForms = todos.length>0?todos.map(item => {
        return {
            instance: Form.useForm()[0],
            data: item.data,
        }
    }):[
        {
            instance: Form.useForm()[0],
            data: undefined,
        }
    ]

    React.useEffect(() => {
        viewForms.forEach(item => {
            const formInstance = item.instance;
            const data = item.data;
            context.getPresenter().getFormActionContext().addAction({
                save(): any {
                    return formInstance.getFieldsValue();
                },
                key(): string {
                    return 'view-form'
                }
            });
            formInstance.setFieldsValue(data);
        });
    }, []);

    if (ViewComponent) {
        if (mergeable) {
            return (
                <div>
                    <h3>合并审批</h3>
                </div>
            )
        }
        return (
            <>
                {viewForms.map((item, index) => (
                    <ViewComponent
                        key={index}
                        form={item.instance}
                        onValuesChange={props.onValuesChange}
                    />
                ))}
            </>
        )
    }
}

export const Body = () => {

    const {state, context} = useApprovalContext();

    const handleValuesChange = (values:any) => {
        context.getPresenter().processNodes().then(nodes => {
            console.log('流程节点:', nodes);
        });
    }

    return (
        <Row>
            <Col span={18}>
                表单详情
                <FormViewComponent
                    onValuesChange={handleValuesChange}
                />
            </Col>
            <Col span={6}>
                流转历史
            </Col>
        </Row>
    )
}