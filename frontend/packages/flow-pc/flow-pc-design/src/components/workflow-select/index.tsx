import React from "react";
import {options} from "@/api/workflow";
import {Button, Modal, Space} from "antd";

interface Select {
    label: string;
    value: string;
}

interface WorkflowSelectModalProps {
    open: boolean;
    onClose: () => void;
    onSelect?:(code: string) => void;
}

export const WorkflowSelectModal: React.FC<WorkflowSelectModalProps> = (props) => {

    const [option, setOption] = React.useState<Select[]>([]);

    React.useEffect(() => {
        options().then(res => {
            if (res.success) {
                setOption(res.data.list);
            }
        })
    }, []);

    return (
        <Modal
            open={props.open}
            onCancel={props.onClose}
            title={"请选择发起流程"}
            width={"45%"}
            footer={false}
        >
            <Space style={{
                marginTop: "10px",
            }}>
                {option.map((item, index) => {
                    return (
                        <Button
                            key={index}
                            type={'primary'}
                            onClick={() => {
                                props.onSelect?.(item.value);
                            }}
                        >
                            {item.label}
                        </Button>
                    )
                })}
            </Space>
        </Modal>
    )
}