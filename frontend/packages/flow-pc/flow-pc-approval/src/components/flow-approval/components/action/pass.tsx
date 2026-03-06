import React from "react";
import {FlowActionProps} from "./type";
import {Button, Form, message, Modal,Input} from "antd";
import {useApprovalContext} from "@/components/flow-approval/hooks/use-approval-context";

const {TextArea} = Input;

export const PassAction: React.FC<FlowActionProps> = (props) => {

    const action = props.action;
    const {state, context} = useApprovalContext()
    const actionPresenter = context.getPresenter().getFlowActionPresenter();

    const [modalVisible, setModalVisible] = React.useState(false);

    const [form] = Form.useForm();


    const handleSubmit = (params?:any) => {
        actionPresenter.action(action.id,params).then((res) => {
            if (res.success) {
                message.success("操作成功");
                setModalVisible(false);
                context.close();
            }
        });
    }

    const adviceRules =state.flow?.adviceRequired? [
        {
            required: state.flow?.adviceRequired || false,
            message:'请输入审批意见'
        }
    ]:[];

    const signRules = state.flow?.signRequired ?[
        {
            required: state.flow?.signRequired || false,
            message:'请设置签名'
        }
    ]:[];

    return (
        <>
            <Button
                onClick={() => {
                    setModalVisible(true);
                }}
            >
                {action.title}
            </Button>

            <Modal
                title={"流程审批"}
                open={modalVisible}
                onCancel={()=>setModalVisible(false)}
                onOk={()=>{
                    form.submit();
                }}
            >
                <Form
                    form={form}
                    layout="vertical"
                    onFinish={(values)=>{
                        handleSubmit(values);
                    }}
                >
                    <Form.Item
                        name={"advice"}
                        label={"审批意见"}
                        required={state.flow?.adviceRequired}
                        rules={adviceRules}
                    >
                        <TextArea placeholder={"请输入审批意见"}/>
                    </Form.Item>

                    <Form.Item
                        name={"signKey"}
                        label={"签名"}
                        required={state.flow?.signRequired}
                        rules={signRules}
                    >
                        <TextArea placeholder={"请输入审批签名"}/>
                    </Form.Item>
                </Form>

            </Modal>
        </>
    )
}