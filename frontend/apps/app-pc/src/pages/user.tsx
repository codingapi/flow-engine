import React from "react";
import {type ActionType, Table, type TableProps} from "@flow-engine/flow-pc-ui";
import {Button, Form, Input, message, Modal, Popconfirm, Space, Switch} from "antd";
import {list, remove, save} from "@/api/user.ts";

const UserPage = () => {
    const actionType = React.useRef<ActionType>(null);
    const [visible, setVisible] = React.useState(false);
    const [form] = Form.useForm();

    const columns: TableProps<any>['columns'] = [
        {
            dataIndex: 'id',
            title: '编号',
        },
        {
            dataIndex: 'name',
            title: '姓名',
        },
        {
            dataIndex: 'account',
            title: '账户',
        },
        {
            dataIndex: 'flowManager',
            title: '角色',
            render: (value, record) => {
                return value ? '流程管理员' : '普通用户'
            }
        },
        {
            dataIndex: 'flowOperatorId',
            title: '流程委托人',
            render: (value, record) => {
                if (!value) {
                    return '无'
                }
                return value
            }
        },
        {
            dataIndex: 'option',
            title: '操作',
            render: (value, record) => {
                return (
                    <Space>
                        <a onClick={() => {
                            form.setFieldsValue({
                                ...record,
                                password: ''
                            });
                            setVisible(true);
                        }}>编辑</a>
                        {record.account !== 'admin' && (
                            <Popconfirm
                                title={"确认要删除该用户吗？"}
                                onConfirm={() => {
                                    remove(record.id).then(() => {
                                        message.success("用户已删除");
                                        actionType.current?.reload();
                                    })
                                }}
                            >
                                <a>删除</a>
                            </Popconfirm>
                        )}
                    </Space>
                )
            }
        }
    ];

    return (
        <div>
            <Table
                rowKey={"id"}
                actionType={actionType}
                toolBarRender={() => {
                    return [
                        <Button
                            key={"create"}
                            type={'primary'}
                            onClick={() => {
                                form.resetFields();
                                setVisible(true);
                            }}>创建用户</Button>
                    ]
                }}
                columns={columns}
                request={(request) => {
                    return list(request);
                }}
            />

            <Modal
                width={"60%"}
                title={"编辑用户"}
                open={visible}
                onCancel={() => {
                    setVisible(false);
                }}
                onOk={() => {
                    form.submit();
                }}
            >
                <Form
                    form={form}
                    onFinish={(values) => {
                        save(values).then(() => {
                            message.success("用户已保存");
                            actionType.current?.reload();
                            setVisible(false);
                        })
                    }}
                >
                    <Form.Item
                        name="id"
                        hidden={true}
                    >
                        <Input/>
                    </Form.Item>

                    <Form.Item
                        name="name"
                        label={"名称"}
                        rules={[
                            {
                                message: '名称不能为空',
                                required: true,
                            }
                        ]}
                    >
                        <Input/>
                    </Form.Item>


                    <Form.Item
                        name="account"
                        label={"登陆账号"}
                        rules={[
                            {
                                message: '登陆账号不能为空',
                                required: true,
                            }
                        ]}
                    >
                        <Input/>
                    </Form.Item>

                    <Form.Item
                        name="password"
                        label={"登陆密码"}
                        rules={[
                            {
                                message: '登陆密码不能为空',
                                required: true,
                            }
                        ]}
                    >
                        <Input/>
                    </Form.Item>

                    <Form.Item
                        name="flowManager"
                        label={"流程管理员"}
                    >
                        <Switch/>
                    </Form.Item>

                    <Form.Item
                        name="flowOperatorId"
                        label={"流程委托人"}
                    >
                        <Input/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default UserPage;