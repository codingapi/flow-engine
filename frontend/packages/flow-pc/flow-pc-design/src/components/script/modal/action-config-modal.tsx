import React from "react";
import {ActionModalProps} from "@/components/script/typings";
import {Modal} from "antd";
import {ActionForm} from "@/components/script/components/action";


export const ActionConfigModal: React.FC<ActionModalProps> = (props) => {
    return (
        <Modal
            open={props.open}
            onCancel={props.onCancel}
            title={"编辑按钮"}
            width={"65%"}
            destroyOnHidden={true}
        >
            <ActionForm
                form={props.form}
                manager={props.manager}
                onFinish={(values) => {
                    props.onFinish(values);
                    props.onCancel();
                }}
            />
        </Modal>
    )
}