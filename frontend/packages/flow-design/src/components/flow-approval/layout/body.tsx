import React from "react";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {Col, Form, Row} from "antd";
import {ViewPlugin} from "@/plugins/view";

export const Body = () => {
    const {state, context} = useApprovalContext();

    const ViewComponent = ViewPlugin.getInstance().get(state.flow?.view || 'default');

    const todos = state.flow?.todos || [];
    const [viewForm] = Form.useForm();

    React.useEffect(() => {
        context.getPresenter().getFormActionContext().addAction({
            save(): any {
                return viewForm.getFieldsValue();
            },
            key(): string {
                return 'view-form'
            }
        })
    }, []);

    return (
        <Row>
            <Col span={18}>
                表单详情

                {ViewComponent && todos.length <= 1 && (
                    <ViewComponent form={viewForm}/>
                )}

            </Col>
            <Col span={6}>
                流转历史
            </Col>
        </Row>
    )
}