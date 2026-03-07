import React from "react";
import {FlowActionProps} from "./type";
import {Form, Input, message, Modal} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";
import {ActionButton} from "@/components/flow-approval/components/action-button";

const {TextArea} = Input;

/**
 * 拒绝
 * @param props
 * @constructor
 */
export const RejectAction: React.FC<FlowActionProps> = (props) => {

    const action = props.action;
    const {state, context} = useApprovalContext()
    const actionPresenter = context.getPresenter().getFlowActionPresenter();
    const [modalVisible, setModalVisible] = React.useState(false);
    const [form] = Form.useForm();

    const handleSubmit = (params?: any) => {
        actionPresenter.action(action.id, params).then((res) => {
            if (res.success) {
                message.success("操作成功");
                setModalVisible(false);
                context.close();
            }
        });
    }

    const adviceRules = state.flow?.adviceRequired ? [
        {
            required: state.flow?.adviceRequired || false,
            message: '请输入审批意见'
        }
    ] : [];

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
                title={"审批拒绝"}
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
                        name={"advice"}
                        label={"拒绝意见"}
                        required={state.flow?.adviceRequired}
                        rules={adviceRules}
                    >
                        <TextArea placeholder={"请输入拒绝意见"}/>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    )
}