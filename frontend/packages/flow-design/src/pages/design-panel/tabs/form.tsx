import React, {useState} from "react";
import {Panel} from "@/components/panel";
import {CardForm} from "@/components/form/card";
import {Table, TableProps} from "@/components/table";
import {Button, Flex, Form, Input,Switch,Select, Modal, Tabs} from "antd";
import {FormFieldOptions} from "@/pages/design-panel/types";


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

    return (
        <Panel>
            <FormTable title={"主表字段"}/>
            <Tabs
                items={[
                    {
                        key: 'name',
                        label: '子表',
                        children: <FormTable title={"子表字段"}/>
                    }
                ]}
                tabBarExtraContent={{
                    right: <Button>添加子表</Button>
                }}
            />
        </Panel>
    )
}