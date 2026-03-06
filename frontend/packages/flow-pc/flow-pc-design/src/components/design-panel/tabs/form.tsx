import React, {useState} from "react";
import {Panel} from "@flow-engine/flow-pc-ui";
import {Table, TableProps} from"@flow-engine/flow-pc-ui";
import {Button, Flex, Form, FormInstance, Input, Modal, Popconfirm, Select, Space, Switch, Tabs,Empty, Row, Col} from "antd";
import {dataTypeOptions} from "@flow-engine/flow-types";
import {useDesignContext} from "@/components/design-panel/hooks/use-design-context";
import {WorkflowFormManager} from "@/components/design-panel/manager/form";

interface FormTableProps {
    name: string;
    code: string;
    delete: boolean;
}

interface FormFieldModalProps {
    open: boolean;
    onClose: () => void;
    onFinish?: (values: any) => void;
    form: FormInstance;
}

const FormFieldModal: React.FC<FormFieldModalProps> = (props) => {

    const form = props.form;

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
            onOk={() => {
                form.submit();
            }}
        >
            <Form
                form={form}
                title={"编辑字段"}
                layout="vertical"
                onFinish={(values: any) => {
                    props.onFinish?.(values);
                    props.onClose?.();
                }}
            >
                <Form.Item
                    name={"id"}
                    hidden={true}
                >
                    <Input/>
                </Form.Item>
                <Row gutter={[8,8]}>
                    <Col span={12}>
                        <Form.Item
                            name={"name"}
                            label={"字段名称"}
                            labelCol={labelCol}
                            rules={[
                                {
                                    required: true,
                                    message: '字段名称不能为空'
                                }
                            ]}
                        >
                            <Input placeholder={"请输入字段名称"}/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"code"}
                            label={"字段编码"}
                            labelCol={labelCol}
                            rules={[
                                {
                                    required: true,
                                    message: '字段编码不能为空'
                                }
                            ]}
                        >
                            <Input placeholder={"请输入字段编码"}/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"type"}
                            label={"字段类型"}
                            labelCol={labelCol}
                            rules={[
                                {
                                    required: true,
                                    message: '字段类型不能为空'
                                }
                            ]}
                        >
                            <Select
                                placeholder={"请输入字段类型"}
                                options={dataTypeOptions}
                            />
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"required"}
                            label={"是否必填"}
                            labelCol={labelCol}
                        >
                            <Switch/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"placeholder"}
                            label={"输入提示信息"}
                            labelCol={labelCol}
                        >
                            <Input placeholder={"请输入输入提示信息"}/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"defaultValue"}
                            label={"默认值"}
                            labelCol={labelCol}
                        >
                            <Input placeholder={"请输入默认值"}/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"tooltip"}
                            label={"提示信息"}
                            labelCol={labelCol}
                        >
                            <Input placeholder={"请输入提示信息"}/>
                        </Form.Item>
                    </Col>
                    <Col span={12}>
                        <Form.Item
                            name={"help"}
                            label={"帮助提示"}
                            labelCol={labelCol}
                        >
                            <Input placeholder={"请输入帮助提示"}/>
                        </Form.Item>
                    </Col>
                </Row>
            </Form>
        </Modal>
    )
}

interface SubFormModalProps {
    open: boolean;
    onClose: () => void;
    onFinish?: (values: any) => void;
    form: FormInstance;
}

const SubFormModal = (props: SubFormModalProps) => {

    const form = props.form;

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
                onFinish={(values: any) => {
                    props.onFinish?.(values);
                    props.onClose();
                }}
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

    const name = props.name;
    const {state, context} = useDesignContext();
    const workflowFormManager = new WorkflowFormManager(state.workflow.form);
    const presenter = context.getPresenter();
    const [fieldForm] = Form.useForm();
    const [editable, setEditable] = useState(false);

    const columns: TableProps<any>['columns'] = [
        {
            dataIndex: 'id',
            title: 'id',
            hidden: true,
        },
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
            title: '字段类型',
            render: (value, record) => {
                let label = '';
                for(const option of dataTypeOptions) {
                    if(option.value == value){
                        label = option.label;
                    }
                }
                return label
            }
        },
        {
            dataIndex: 'required',
            title: '是否为空',
            render: (value) => {
                return value ? '必填' : '非必填'
            }
        },
        {
            dataIndex: 'placeholder',
            title: '输入提示'
        },
        {
            dataIndex: 'defaultValue',
            title: '默认值'
        },
        {
            dataIndex: 'tooltip',
            title: '提示信息'
        },
        {
            dataIndex: 'help',
            title: '帮助提示'
        },
        {
            dataIndex: 'option',
            title: '操作',
            render: (_, record) => {
                return (
                    <Space>
                        <a onClick={() => {
                            fieldForm.resetFields();
                            fieldForm.setFieldsValue(record);
                            setEditable(true);
                        }}>编辑</a>
                        <Popconfirm
                            title={"确认要删除该字段吗？"}
                            onConfirm={() => {
                                presenter.removeWorkflowFormField(props.code, record.code);
                            }}
                        >
                            <a>删除</a>
                        </Popconfirm>
                    </Space>
                )
            }
        },
    ];


    return (
        <>
            <Table
                columns={columns}
                rowKey={"id"}
                dataSource={workflowFormManager.getFormFields(props.code)}
                title={() => {
                    return (
                        <Flex
                            justify={'space-between'}
                            align={'center'}
                        >
                            <Space>
                                {name}
                            </Space>
                            <Space>
                                {props.delete && (
                                    <Popconfirm
                                        title={"确认要删除子表吗？"}
                                        onConfirm={() => {
                                            presenter.removeWorkflowSubForm(props.code);
                                        }}
                                    >
                                        <Button color={'danger'} type={'dashed'} variant="solid">删除子表</Button>
                                    </Popconfirm>
                                )}
                                <Button onClick={() => {
                                    fieldForm.resetFields();
                                    setEditable(true);
                                }}>添加字段</Button>
                            </Space>

                        </Flex>
                    )
                }}
            />
            <FormFieldModal
                open={editable}
                form={fieldForm}
                onClose={() => {
                    setEditable(false);
                }}
                onFinish={(values) => {
                    presenter.updateWorkflowFormField(props.code, values);
                    setEditable(false);
                }}
            />
        </>
    )
}

export const TabForm = () => {

    const [subFormVisible, setSubFormVisible] = useState(false);
    const [subForm] = Form.useForm();
    const {state, context} = useDesignContext();

    const presenter = context.getPresenter();

    const mainCode = state.workflow.form.code;
    const mainName = state.workflow.form.name;

    const subForms = state.workflow.form.subForms || [];

    const items = subForms.map(item => {
        const title = `子表:${item.name}`;
        return {
            key: item.code,
            label: title,
            children: <FormTable name={title} code={item.code} delete={true}/>
        }
    });

    if(!mainCode){
        return (
            <Empty description={"请先在基本信息中添加表单的定义配置."}/>
        )
    }

    return (
        <Panel>
            <FormTable name={`主表:${mainName}`} code={mainCode} delete={false}/>
            <Tabs
                style={{
                    marginTop:20
                }}
                items={items}
                tabBarExtraContent={{
                    right: (
                        <Button
                            onClick={() => {
                                subForm.resetFields();
                                setSubFormVisible(true)
                            }}>
                            添加子表
                        </Button>
                    )
                }}
            />

            <SubFormModal
                form={subForm}
                open={subFormVisible}
                onFinish={(values) => {
                    presenter.addWorkflowSubForm(values);
                }}
                onClose={() => {
                    setSubFormVisible(false)
                }}
            />
        </Panel>
    )
}