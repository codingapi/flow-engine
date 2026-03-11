import React from "react";
import {Button, Form, Input, message, Popover, Space, Tabs} from "antd";
import {LayoutHeaderHeight, TabPanelType} from "../types";
import {useDesignContext} from "../hooks/use-design-context";
import {CloseOutlined, SaveOutlined} from "@ant-design/icons";
import {EventBus} from "@flow-engine/flow-core";

const Left = () => {
    return (
        <div style={{
            width: 150,
        }}>流程设计面板</div>
    )
}


const SaveAsButton = () => {

    const [visible, setVisible] = React.useState(false);
    const [form] = Form.useForm();
    const {context} = useDesignContext();

    React.useEffect(()=>{
        if(!visible){
            form.resetFields();
        }
    },[visible]);

    return (
        <Popover
            placement={"bottom"}
            trigger={"click"}
            open={visible}
            content={() => {
                return (
                    <div>
                        <Form
                            layout={"vertical"}
                            form={form}
                            onFinish={(values)=>{
                                context.save(values.name).then(()=>{
                                    setVisible(false);
                                    message.success('版本已保存');

                                    EventBus.getInstance().emit('VersionChangeEvent');
                                });
                            }}
                        >
                            <Form.Item
                                name="name"
                                label={"版本名称"}
                                required={true}
                                rules={[
                                    {
                                        required: true,
                                        message:'请输入版本名称'
                                    }
                                ]}
                            >
                                <Input
                                    placeholder={"请输入版本名称"}
                                />
                            </Form.Item>
                        </Form>
                        <Space
                            style={{
                                marginTop: 8,
                            }}
                        >
                            <Button
                                onClick={()=>{
                                    form.submit();
                                }}
                                type="primary"
                            >确定</Button>
                            <Button
                                onClick={() => {
                                    setVisible(false);
                                }}
                            >取消</Button>
                        </Space>
                    </div>
                )
            }}
        >
            <Button
                icon={<SaveOutlined/>}
                onClick={() => {
                    setVisible(true)
                }}
            >
                版本另存
            </Button>
        </Popover>
    )
}

const Right = () => {
    const {context} = useDesignContext();

    return (
        <Space style={{
            marginRight: 20
        }}>

            <Button
                icon={<SaveOutlined/>}
                type="primary"
                onClick={() => {
                    context.save().then(() => {
                        message.success("流程已经保存.");
                    });
                }}
            >保存</Button>
            <SaveAsButton/>
            <Button
                icon={<CloseOutlined/>}
                onClick={() => {
                    context.close();
                }}
            >关闭</Button>
        </Space>
    )
}

export const Header = () => {

    const {state, context} = useDesignContext();

    return (
        <Tabs
            style={{
                width: "100%",
                height: LayoutHeaderHeight,
            }}
            destroyOnHidden={true}
            centered={true}
            items={[
                {
                    key: 'base',
                    label: '基本信息',
                },
                {
                    key: 'form',
                    label: '表单设计',
                },
                {
                    key: 'flow',
                    label: '流程设计',
                },
                {
                    key: 'setting',
                    label: '更多参数',
                },
            ]}
            defaultActiveKey={state.view.tabPanel}
            onChange={(key) => {
                context.getPresenter().updateViewPanelTab(key as TabPanelType);
            }}
            tabBarExtraContent={{
                left: <Left/>,
                right: <Right/>,
            }}
        />
    )
}