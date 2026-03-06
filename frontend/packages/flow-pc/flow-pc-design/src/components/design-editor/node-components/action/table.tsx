import React from "react";
import {Button, Form, Space, Switch,Popconfirm} from "antd";
import {Table} from "@flow-engine/flow-pc-ui";
import {ActionManager} from "@/components/design-editor/node-components/action/index";
import {useNodeRenderContext} from "@/components/design-editor/hooks/use-node-render-context";
import {PlusOutlined} from "@ant-design/icons";
import {ActionModal} from "@/components/design-editor/node-components/action/modal";
import {GroovyScriptConvertorUtil} from "@flow-engine/flow-core";

interface ActionTableProps {
    value: any[];
    onChange: (data: any[]) => void;
}

export const ActionTable: React.FC<ActionTableProps> = (props) => {
    const {node} = useNodeRenderContext();
    const actions = node.getNodeRegistry()?.meta.actions || [];
    const actionManager = new ActionManager(props.value, props.onChange);
    const datasource = actionManager.getDatasource(actions);
    const [visible, setVisible] = React.useState(false);
    const [customVisible, setCustomVisible] = React.useState(false);
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
                title: '启用',
                dataIndex: 'enable',
                key: 'enable',
                render: (_: any, record: any) => {
                    return (
                        <Switch
                            size="small"
                            value={record.enable}
                            onChange={(value) => {
                                actionManager.enable(record.id, value);
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
                                        const custom = record.type==='CUSTOM';
                                        let trigger = {};
                                        if(custom){
                                            const meta = GroovyScriptConvertorUtil.getScriptMeta(record.script);
                                            trigger = JSON.parse(meta);
                                        }
                                        const data = {
                                            ...record,
                                            ...record.display,
                                            ...trigger,
                                            title: record.title,
                                            id: record.id,
                                        }
                                        form.setFieldsValue(data);
                                        setCustomVisible(custom);
                                        setVisible(true);
                                    }}
                                >
                                    编辑
                                </a>
                                {record.type==='CUSTOM' && (
                                    <Popconfirm
                                        title={"确认删除吗？"}
                                        onConfirm={()=>{
                                            actionManager.delete(record.id);
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
                            setCustomVisible(true);
                            setVisible(true);
                        }}
                    >添加按钮</Button>
                ]}
                columns={columns()}
                dataSource={datasource}
                style={{
                    width: "100%",
                }}
                pagination={false}
            />

            <ActionModal
                open={visible}
                form={form}
                options={actionManager.getActionOptions()}
                custom={customVisible}
                onCancel={() => {
                    setVisible(false);
                }}
                onFinish={(values) => {
                    actionManager.update(values);
                }}
            />
        </>
    )
}