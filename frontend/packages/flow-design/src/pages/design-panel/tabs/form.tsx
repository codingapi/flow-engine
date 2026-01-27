import React, {useState} from "react";
import {Panel} from "@/components/panel";
import {CardForm} from "@/components/form/card";
import {Table, TableProps} from "@/components/table";
import {Button, Flex, Form, Input, Switch, Select, Modal, Tabs, Space,Popconfirm} from "antd";
import {FormFieldOptions} from "@/pages/design-panel/types";
import { CloseOutlined } from "@ant-design/icons";
import {useDesignContext} from "@/pages/design-panel/hooks/use-design-context";

interface FormTableProps {
    title: string;
}

interface FormFieldModalProps {
    open: boolean;
    onClose: () => void;
}

const FormFieldModal: React.FC<FormFieldModalProps> = (props) => {

    const [form] = Form.useForm();

    const labelCol = {
        style: {
            width: 100
        }
    };

    return (
        <Modal
            open={props.open}
            title={"编辑字段"}
            width={"60%"}
            onCancel={props.onClose}
        >
            <Form
                form={form}
                title={"编辑字段"}
                layout="vertical"
            >
                <Form.Item
                    name={"name"}
                    label={"字段名称"}
                    labelCol={labelCol}
                >
                    <Input placeholder={"请输入字段名称"}/>
                </Form.Item>

                <Form.Item
                    name={"code"}
                    label={"字段编码"}
                    labelCol={labelCol}
                >
                    <Input placeholder={"请输入字段编码"}/>
                </Form.Item>

                <Form.Item
                    name={"type"}
                    label={"字段类型"}
                    labelCol={labelCol}
                >
                    <Select
                        placeholder={"请输入字段类型"}
                        options={FormFieldOptions}
                    />
                </Form.Item>

                <Form.Item
                    name={"required"}
                    label={"是否必填"}
                    labelCol={labelCol}
                >
                    <Switch/>
                </Form.Item>

                <Form.Item
                    name={"defaultValue"}
                    label={"默认值"}
                    labelCol={labelCol}
                >
                    <Input placeholder={"请输入默认值"}/>
                </Form.Item>
            </Form>

        </Modal>
    )
}

interface SubFormModalProps {
    open: boolean;
    onClose: () => void;
}

const SubFormModal = (props: SubFormModalProps) => {

    const [form] = Form.useForm();

    return (
        <Modal
            title={"添加子表"}
            width={"60%"}
            open={props.open}
            onCancel={props.onClose}
            onOk={() => {
                form.submit();
            }}
        >
            <Form
                form={form}
                layout="vertical"
            >
                <Form.Item
                    name={"name"}
                    label={"子表名称"}
                    rules={[
                        {
                            required: true,
                            message: '子表名称不能为空'
                        }
                    ]}
                >
                    <Input placeholder={"添加子表名称"}/>

                </Form.Item>

                <Form.Item
                    name={"code"}
                    label={"子表编码"}
                    rules={[
                        {
                            required: true,
                            message: '子表编码不能为空'
                        }
                    ]}
                >
                    <Input placeholder={"添加子表编码"}/>

                </Form.Item>
            </Form>
        </Modal>
    )
}

const FormTable: React.FC<FormTableProps> = (props) => {

    const title = props.title;
    const columns: TableProps<any>['columns'] = [
        {
            dataIndex: 'name',
            title: '字段名称',
        },
        {
            dataIndex: 'code',
            title: '字段编码'
        },
        {
            dataIndex: 'type',
            title: '字段类型'
        },
        {
            dataIndex: 'nullable',
            title: '是否为空'
        },
        {
            dataIndex: 'defaultValue',
            title: '默认值'
        }
    ];
    const [editable, setEditable] = useState(false);

    return (
        <>
            <Table
                columns={columns}
                key={"code"}
                title={() => {
                    return (
                        <Flex
                            justify={'space-between'}
                            align={'center'}
                        >
                            <div>{title}</div>
                            <Button onClick={() => {
                                setEditable(true);
                            }}>添加字段</Button>
                        </Flex>
                    )
                }}
            />
            <FormFieldModal
                open={editable}
                onClose={() => {
                    setEditable(false);
                }}
            />
        </>
    )
}

export const TabForm = () => {

    const [form] = CardForm.useForm();

    const [subFormVisible, setSubFormVisible] = useState(false);

    return (
        <Panel>
            <FormTable title={"主表字段"}/>
            <Tabs
                items={[
                    {
                        key: 'name',
                        label: (
                            <Space>
                                子表
                                <Popconfirm
                                    title={"确认要删除子表吗？"}
                                    onConfirm={()=>{

                                    }}
                                >
                                    <CloseOutlined/>
                                </Popconfirm>
                            </Space>
                        ),
                        children: <FormTable title={"子表字段"}/>
                    }
                ]}
                tabBarExtraContent={{
                    right: <Button onClick={() => {
                        setSubFormVisible(true)
                    }}>添加子表</Button>
                }}
            />

            <SubFormModal
                open={subFormVisible}
                onClose={() => {
                    setSubFormVisible(false)
                }}
            />
        </Panel>
    )
}