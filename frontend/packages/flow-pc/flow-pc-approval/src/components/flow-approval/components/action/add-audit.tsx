import React from "react";
import {FlowActionProps} from "./type";
import {Button, Form, message, Modal} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {AddAuditView} from "@/components/flow-approval/plugins/view/add-audit-view";

/**
 * 加签
 * @param props
 * @constructor
 */
export const AddAuditAction: React.FC<FlowActionProps> = (props) => {

    const action = props.action;
    const {context} = useApprovalContext();
    const [form] = Form.useForm();

    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    const [modalVisible, setModalVisible] = React.useState(false);

    const handleSubmit = (params?: any) => {
        actionPresenter.action(action.id, params).then((res) => {
            if (res.success) {
                message.success("操作成功");
                setModalVisible(false);
                context.close();
            }
        });
    }
    return (
        <>
            <Button
                onClick={() => {
                    form.resetFields();
                    setModalVisible(true);
                }}
            >
                {action.title}
            </Button>

            <Modal
                title={"加签审批"}
                open={modalVisible}
                onCancel={() => setModalVisible(false)}
                onOk={() => {
                    form.submit();
                }}
            >
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={(values) => {
                        handleSubmit(values);
                    }}
                >
                    <Form.Item
                        name={"forwardOperatorIds"}
                        label={"加签人员"}
                        required={true}
                        rules={[
                            {
                                required: true,
                                message:'加签人员不能为空'
                            }
                        ]}
                    >
                        <AddAuditView/>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    )
}