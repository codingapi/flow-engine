import React from "react";
import {Button, Form, Popconfirm, Space, Switch} from "antd";
import {Table} from "@flow-engine/flow-pc-ui";
import {useNodeRenderContext} from "@/components/design-editor/hooks/use-node-render-context";
import {PlusOutlined} from "@ant-design/icons";
import {actionOptions} from "@flow-engine/flow-types";
import {ActionConfigModal} from "@/components/script/modal/action-config-modal";
import {FlowActionListPresenter} from "./presenter";

interface ActionTableProps {
    value: any[];
    onChange: (data: any[]) => void;
}

export const ActionTable: React.FC<ActionTableProps> = (props) => {
    const {node} = useNodeRenderContext();
    const actions = node.getNodeRegistry()?.meta.actions || [];
    const presenter = new FlowActionListPresenter(props.value, props.onChange);
    const datasource = presenter.getDatasource(actions);
    const [visible, setVisible] = React.useState(false);
    const [form] = Form.useForm();

    const columns = React.useCallback(() => {
        return [
            {
                title: 'id',
                dataIndex: 'id',
                key: 'id',
                hidden: true,
            },
            {
                title: '标题',
                dataIndex: 'title',
                key: 'title',
            },
            {
                title: '类型',
                dataIndex: 'type',
                key: 'type',
                render:(value:string) => {
                    const type = actionOptions.find(item=>item.value === value);
                    return type?.label
                }
            },
            {
                title: '启用',
                dataIndex: 'enable',
                key: 'enable',
                render: (_: any, record: any) => {
                    return (
                        <Switch
                            size="small"
                            value={record.enable}
                            onChange={(value) => {
                                presenter.enable(record.id, value);
                            }}
                        />
                    )
                }
            },
            {
                title: '操作',
                dataIndex: 'operation',
                key: 'operation',
                render: (_: any, record: any) => {
                    if(record.enable) {
                        return (
                            <Space>
                                <a
                                    onClick={() => {
                                        form.setFieldsValue(record);
                                        setVisible(true);
                                    }}
                                >
                                    编辑
                                </a>
                                {record.type==='CUSTOM' && (
                                    <Popconfirm
                                        title={"确认删除吗？"}
                                        onConfirm={()=>{
                                            presenter.delete(record.id);
                                        }}
                                    >
                                        <a>
                                            删除
                                        </a>
                                    </Popconfirm>
                                )}
                            </Space>
                        )
                    }
                }
            },
        ];
    }, [props.value]);

    return (
        <>
            <Table
                toolBarRender={() => [
                    <Button
                        icon={<PlusOutlined/>}
                        onClick={() => {
                            form.resetFields();
                            setVisible(true);
                        }}
                    >自定义按钮</Button>
                ]}
                columns={columns()}
                dataSource={datasource}
                style={{
                    width: "100%",
                }}
                pagination={false}
            />
            <ActionConfigModal
                open={visible}
                manager={presenter.getFlowActionManager()}
                form={form}
                onCancel={() => {
                    setVisible(false);
                }}
                onFinish={(values) => {
                    presenter.update(values);
                }}
            />
        </>
    )
}