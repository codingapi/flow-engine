import React from "react";
import {FlowActionProps} from "./type";
import {Form, message, Modal} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {AddAuditView} from "@/plugins/view/add-audit-view";
import {ActionButton} from "@/components/flow-approval/components/action-button";

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
            <ActionButton
                display={props.action.display}
                onClick={() => {
                    form.resetFields();
                    setModalVisible(true);
                }}
                title={action.title}
            />

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